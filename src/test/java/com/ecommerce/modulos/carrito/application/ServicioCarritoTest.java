package com.ecommerce.modulos.carrito.application;

import com.ecommerce.modulos.carrito.domain.ArticuloCarrito;
import com.ecommerce.modulos.carrito.domain.Carrito;
import com.ecommerce.modulos.catalogo.domain.Producto;
import com.ecommerce.modulos.catalogo.domain.RepositorioProducto;
import com.ecommerce.modulos.compartido.domain.Dinero;
import com.ecommerce.modulos.compartido.domain.ExcepcionEntidadNoEncontrada;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServicioCarritoTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private RepositorioProducto repositorioProducto;

    @InjectMocks
    private ServicioCarrito servicioCarrito;

    private UUID userId;
    private UUID idTienda;
    private UUID idProducto;
    private ArticuloCarrito item;
    private Producto productoMoc;

    private String clave;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        idTienda = UUID.randomUUID();
        idProducto = UUID.randomUUID();
        clave = "carrito:" + idTienda + ":" + userId;

        item = new ArticuloCarrito();
        item.setIdProducto(idProducto);
        item.setIdTienda(idTienda);
        item.setCantidad(2);

        productoMoc = new Producto();
        productoMoc.setIdTienda(idTienda);
        productoMoc.setNombre("Producto Test");
        productoMoc.setPrecio(Dinero.of(new BigDecimal("10.00"), "USD"));
    }

    @Test
    void debeObtenerOCrearCarrito() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(clave)).thenReturn(null); // No existe

        Carrito carrito = servicioCarrito.getOrCreateCart(userId, idTienda);

        assertNotNull(carrito);
        assertEquals(clave, carrito.getId());
        assertEquals(idTienda, carrito.getIdTienda());
    }

    @Test
    @DisplayName("El mismo usuario en dos tiendas usa claves de Redis distintas (aislamiento multi-tenant)")
    void debeAislarCarritosPorTienda() {
        UUID tiendaA = UUID.randomUUID();
        UUID tiendaB = UUID.randomUUID();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        servicioCarrito.getOrCreateCart(userId, tiendaA);
        servicioCarrito.getOrCreateCart(userId, tiendaB);

        verify(valueOperations).get("carrito:" + tiendaA + ":" + userId);
        verify(valueOperations).get("carrito:" + tiendaB + ":" + userId);
    }

    @Test
    void debeAgregarArticuloYValidarConBaseDeDatos() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(clave)).thenReturn(null);
        when(repositorioProducto.findByIdWithVariants(idProducto)).thenReturn(Optional.of(productoMoc));

        Carrito carrito = servicioCarrito.agregarArticulo(userId, idTienda, item);

        assertNotNull(carrito);
        assertEquals(1, carrito.getArticulos().size());
        assertEquals(new BigDecimal("10.00"), carrito.getArticulos().get(0).getPrecioUnitario());
        assertEquals(new BigDecimal("20.00"), carrito.getArticulos().get(0).getSubtotal());

        verify(valueOperations).set(eq(clave), any(Carrito.class), any());
    }

    @Test
    void debeLanzarExcepcionAlAgregarProductoInexistente() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(clave)).thenReturn(null);
        when(repositorioProducto.findByIdWithVariants(idProducto)).thenReturn(Optional.empty());

        assertThrows(ExcepcionEntidadNoEncontrada.class, () -> {
            servicioCarrito.agregarArticulo(userId, idTienda, item);
        });

        verify(valueOperations, never()).set(anyString(), any(Carrito.class), any());
    }

    @Test
    void debeLanzarExcepcionSiProductoEsDeOtraTienda() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(clave)).thenReturn(null);

        productoMoc.setIdTienda(UUID.randomUUID()); // Otra tienda
        when(repositorioProducto.findByIdWithVariants(idProducto)).thenReturn(Optional.of(productoMoc));

        assertThrows(IllegalStateException.class, () -> {
            servicioCarrito.agregarArticulo(userId, idTienda, item);
        });
    }

    @Test
    void debeLimpiarCarritoConClaveDeLaTienda() {
        servicioCarrito.clearCart(userId, idTienda);
        verify(redisTemplate).delete(clave);
    }
}
