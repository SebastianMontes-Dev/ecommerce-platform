package com.ecommerce.modulos.ordenes.application;

import com.ecommerce.modulos.carrito.application.ServicioCarrito;
import com.ecommerce.modulos.carrito.domain.ArticuloCarrito;
import com.ecommerce.modulos.carrito.domain.Carrito;
import com.ecommerce.modulos.identidad.domain.RepositorioUsuario;
import com.ecommerce.modulos.identidad.domain.Usuario;
import com.ecommerce.modulos.ordenes.application.dto.SolicitudCheckout;
import com.ecommerce.modulos.ordenes.domain.Orden;
import com.ecommerce.modulos.ordenes.domain.RepositorioOrden;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest
@Testcontainers
@Transactional
public class CasoUsoOrdenIntegrationTest {

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
    private CasoUsoOrden casoUsoOrden;

    @Autowired
    private RepositorioOrden repositorioOrden;

    @MockBean
    private ServicioCarrito servicioCarrito;

    @MockBean
    private RepositorioUsuario repositorioUsuario;

    private UUID idCliente;
    private UUID idTienda;

    @BeforeEach
    void setUp() {
        idCliente = UUID.randomUUID();
        idTienda = UUID.randomUUID();
    }

    @Test
    void testCreacionOrdenGuardaEnBD() {
        Usuario usuario = new Usuario();
        usuario.setId(idCliente);
        usuario.setCorreo("it@test.com");
        usuario.setNombre("Integration");
        usuario.setApellido("Test");
        when(repositorioUsuario.findById(idCliente)).thenReturn(Optional.of(usuario));

        Carrito carrito = new Carrito();
        ArticuloCarrito articulo = new ArticuloCarrito();
        articulo.setIdProducto(UUID.randomUUID());
        articulo.setCantidad(2);
        articulo.setPrecioUnitario(new BigDecimal("25.00"));
        articulo.setMoneda("USD");
        articulo.setNombreProducto("Producto BD");
        carrito.agregarArticulo(articulo);
        
        when(servicioCarrito.getOrCreateCart(idCliente, idTienda)).thenReturn(carrito);

        SolicitudCheckout request = new SolicitudCheckout();
        request.setDireccionEnvio(com.ecommerce.modulos.compartido.domain.Direccion.of("123 Test St", "City", "State", "00000", "US"));
        request.setDireccionFacturacion(com.ecommerce.modulos.compartido.domain.Direccion.of("123 Test St", "City", "State", "00000", "US"));
        request.setNotas("Prueba de integración");

        RespuestaOrden respuesta = casoUsoOrden.createOrderFromCart(idCliente, idTienda, request);

        assertNotNull(respuesta);
        assertNotNull(respuesta.getId());
        
        Optional<Orden> ordenGuardada = repositorioOrden.findById(respuesta.getId());
        assertTrue(ordenGuardada.isPresent());
        assertEquals(idCliente, ordenGuardada.get().getIdCliente());
        assertEquals(new BigDecimal("50.00"), ordenGuardada.get().getTotal().getMonto());
    }
}
