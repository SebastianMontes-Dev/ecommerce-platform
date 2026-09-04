package com.ecommerce.modulos.compartido.infrastructure;

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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class SpoofingHeaderInquilinoIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void unVendedorNoPuedeCrearProductosEnLaTiendaDeOtroVendedorSpoofeandoElHeader() throws Exception {
        String tokenVendedorA = registrarLoguearYCrearTienda("vendedor-a@test.com", "Tienda A", "tienda-a");
        String tokenVendedorB = registrarLoguearYCrearTienda("vendedor-b@test.com", "Tienda B", "tienda-b");

        String bodyTiendaB = mockMvc.perform(get("/api/v1/inquilinos/tienda-b"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String idTiendaB = JsonPath.read(bodyTiendaB, "$.id");

        Map<String, Object> nuevoProducto = Map.of(
                "nombre", "Producto atacante",
                "enlaceCorto", "producto-atacante",
                "precio", 10.00
        );

        String respuestaCreacion = mockMvc.perform(post("/api/v1/catalogo/productos")
                        .header("Authorization", "Bearer " + tokenVendedorA)
                        .header("X-Inquilino-ID", idTiendaB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nuevoProducto)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String idTiendaDelProductoCreado = JsonPath.read(respuestaCreacion, "$.idTienda");

        assertNotEquals(idTiendaB, idTiendaDelProductoCreado,
                "el producto no debe haberse creado en la tienda spoofeada");

        mockMvc.perform(get("/api/v1/catalogo/productos")
                        .header("X-Inquilino-ID", idTiendaB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", org.hamcrest.Matchers.empty()));
    }

    private String registrarLoguearYCrearTienda(String correo, String nombreTienda, String enlaceCorto) throws Exception {
        Map<String, Object> registro = Map.of(
                "correo", correo,
                "contrasena", "Password123!",
                "confirmarContrasena", "Password123!",
                "nombre", "Test",
                "apellido", "Vendedor"
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
        String token = JsonPath.read(loginResponse, "$.accessToken");

        Map<String, Object> tienda = Map.of("nombre", nombreTienda, "enlaceCorto", enlaceCorto);
        mockMvc.perform(post("/api/v1/inquilinos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tienda)))
                .andExpect(status().isCreated());

        return token;
    }
}
