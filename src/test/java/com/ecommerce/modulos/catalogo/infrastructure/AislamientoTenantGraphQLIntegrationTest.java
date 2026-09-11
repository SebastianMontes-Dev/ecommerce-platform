package com.ecommerce.modulos.catalogo.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ControladorGraphQLProducto dejaba constancia en un comentario de no estar seguro de si el
 * aislamiento de tenant aplicaba a /graphql. Este test lo confirma empíricamente: los mismos
 * filtros globales (FiltroAutenticacionJwt, FiltroInquilino) que protegen la API REST no están
 * restringidos por URL, así que también corren para /graphql — un cliente autenticado en la
 * tienda A no puede leer un producto de la tienda B por más que conozca su ID.
 */
@SpringBootTest(classes = com.ecommerce.bootstrap.AplicacionEcommerce.class)
@AutoConfigureMockMvc
@Testcontainers
class AislamientoTenantGraphQLIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
        registry.add("app.outbox.intervalo-ms", () -> "3600000");
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        registry.add("spring.security.oauth2.client.registration.google.client-id", () -> "test-client-id");
        registry.add("spring.security.oauth2.client.registration.google.client-secret", () -> "test-client-secret");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void unClienteNoPuedeLeerPorGraphqlUnProductoDeOtraTiendaAunqueConozcaElId() throws Exception {
        String tokenVendedorA = registrarLoguearYCrearTienda("dueno-a@test.com", "Tienda GraphQL A", "tienda-graphql-a");
        String tokenVendedorB = registrarLoguearYCrearTienda("dueno-b@test.com", "Tienda GraphQL B", "tienda-graphql-b");
        String tokenCliente = registrarYLoguear("cliente-graphql@test.com");

        String idTiendaA = crearTiendaYObtenerId("tienda-graphql-a");
        String idTiendaB = crearTiendaYObtenerId("tienda-graphql-b");
        String idProductoA = crearProducto(tokenVendedorA, "Producto GraphQL A", "producto-graphql-a");
        String idProductoB = crearProducto(tokenVendedorB, "Producto GraphQL B", "producto-graphql-b");

        // El cliente, scopeado a la tienda A, SÍ ve el producto de la tienda A.
        String respuestaPropia = consultarProductoPorGraphql(tokenCliente, idTiendaA, idProductoA);
        assertEquals("Producto GraphQL A", JsonPath.read(respuestaPropia, "$.data.obtenerProductoPorId.nombre"));

        // El mismo cliente, todavía scopeado a la tienda A, NO puede leer el producto de la
        // tienda B por más que conozca su ID -- ni con el header apuntando a la B lo consigue
        // desde una sesión que no reconoce esa tienda como la propia--.
        String respuestaCruzada = consultarProductoPorGraphql(tokenCliente, idTiendaA, idProductoB);
        assertNull(JsonPath.read(respuestaCruzada, "$.data.obtenerProductoPorId"),
                "un cliente scopeado a la tienda A no debe poder leer un producto de la tienda B");

        // Scopeado correctamente a la tienda B, sí lo ve -- confirma que el mecanismo aísla,
        // no que simplemente rompe todo.
        String respuestaCorrecta = consultarProductoPorGraphql(tokenCliente, idTiendaB, idProductoB);
        assertEquals("Producto GraphQL B", JsonPath.read(respuestaCorrecta, "$.data.obtenerProductoPorId.nombre"));
    }

    private String consultarProductoPorGraphql(String token, String idTienda, String idProducto) throws Exception {
        Map<String, Object> body = Map.of(
                "query", "query($id: ID!) { obtenerProductoPorId(id: $id) { id nombre idTienda } }",
                "variables", Map.of("id", idProducto)
        );
        return mockMvc.perform(post("/graphql")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Inquilino-ID", idTienda)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private String crearProducto(String tokenVendedor, String nombre, String enlaceCorto) throws Exception {
        Map<String, Object> producto = Map.of(
                "nombre", nombre,
                "enlaceCorto", enlaceCorto,
                "precio", 15.00,
                "inventario", 5
        );
        String respuesta = mockMvc.perform(post("/api/v1/catalogo/productos")
                        .header("Authorization", "Bearer " + tokenVendedor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(producto)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.id");
    }

    private String crearTiendaYObtenerId(String enlaceCorto) throws Exception {
        String respuesta = mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/inquilinos/" + enlaceCorto))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.id");
    }

    private String registrarYLoguear(String correo) throws Exception {
        Map<String, Object> registro = Map.of(
                "correo", correo,
                "contrasena", "Password123!",
                "confirmarContrasena", "Password123!",
                "nombre", "Test",
                "apellido", "Cliente"
        );
        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registro)))
                .andExpect(status().isCreated());

        Map<String, Object> login = Map.of("correo", correo, "contrasena", "Password123!");
        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(loginResponse, "$.accessToken");
    }

    private String registrarLoguearYCrearTienda(String correo, String nombreTienda, String enlaceCorto) throws Exception {
        String token = registrarYLoguear(correo);

        Map<String, Object> tienda = Map.of("nombre", nombreTienda, "enlaceCorto", enlaceCorto);
        mockMvc.perform(post("/api/v1/inquilinos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tienda)))
                .andExpect(status().isCreated());

        return token;
    }
}
