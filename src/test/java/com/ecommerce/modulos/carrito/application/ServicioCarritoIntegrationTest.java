package com.ecommerce.modulos.carrito.application;

import com.ecommerce.modulos.carrito.domain.ArticuloCarrito;
import com.ecommerce.modulos.carrito.domain.Carrito;
import com.ecommerce.modulos.catalogo.domain.Producto;
import com.ecommerce.modulos.catalogo.domain.RepositorioProducto;
import com.ecommerce.modulos.compartido.domain.Dinero;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = com.ecommerce.bootstrap.AplicacionEcommerce.class)
@Testcontainers
public class ServicioCarritoIntegrationTest {

    @Container
    public static GenericContainer<?> redis = new GenericContainer<>("redis:6-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private ServicioCarrito servicioCarrito;

    @MockBean
    private RepositorioProducto repositorioProducto;

    private UUID userId;
    private UUID idTienda;
    private UUID idProducto;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        idTienda = UUID.randomUUID();
        idProducto = UUID.randomUUID();
        servicioCarrito.clearCart(userId);
    }

    @Test
    void testFlujoCompletoCarrito() {
        Producto productoMoc = new Producto();
        productoMoc.setId(idProducto);
        productoMoc.setIdTienda(idTienda);
        productoMoc.setNombre("Producto IT");
        productoMoc.setPrecio(Dinero.of(new BigDecimal("15.00"), "USD"));

        when(repositorioProducto.findByIdWithVariants(idProducto)).thenReturn(Optional.of(productoMoc));

        ArticuloCarrito item = new ArticuloCarrito();
        item.setIdProducto(idProducto);
        item.setIdTienda(idTienda);
        item.setCantidad(2);

        Carrito carrito = servicioCarrito.agregarArticulo(userId, idTienda, item);

        assertNotNull(carrito);
        assertEquals(1, carrito.getArticulos().size());
        assertEquals(new BigDecimal("15.00"), carrito.getArticulos().get(0).getPrecioUnitario());

        // Recuperar del redis
        Carrito carritoRecuperado = servicioCarrito.getOrCreateCart(userId, idTienda);
        assertEquals(1, carritoRecuperado.getArticulos().size());
        assertEquals("Producto IT", carritoRecuperado.getArticulos().get(0).getNombreProducto());
    }
}
