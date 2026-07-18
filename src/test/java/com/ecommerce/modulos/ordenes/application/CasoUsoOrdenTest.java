package com.ecommerce.modulos.ordenes.application;

import com.ecommerce.modulos.carrito.application.ServicioCarrito;
import com.ecommerce.modulos.carrito.domain.ArticuloCarrito;
import com.ecommerce.modulos.carrito.domain.Carrito;
import com.ecommerce.modulos.compartido.domain.ExcepcionEntidadNoEncontrada;
import com.ecommerce.modulos.compartido.domain.ExcepcionOperacionInvalida;
import com.ecommerce.modulos.compartido.domain.PublicadorEventoDominio;
import com.ecommerce.modulos.identidad.domain.RepositorioUsuario;
import com.ecommerce.modulos.identidad.domain.Usuario;
import com.ecommerce.modulos.ordenes.application.dto.SolicitudCheckout;
import com.ecommerce.modulos.ordenes.domain.Orden;
import com.ecommerce.modulos.ordenes.domain.RepositorioOrden;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CasoUsoOrdenTest {

    @Mock
    private RepositorioOrden repositorioOrden;
    @Mock
    private ServicioCarrito servicioCarrito;
    @Mock
    private RepositorioUsuario repositorioUsuario;
    @Mock
    private PublicadorEventoDominio eventPublisher;

    @InjectMocks
    private CasoUsoOrden casoUsoOrden;

    private UUID idCliente;
    private UUID idTienda;
    private SolicitudCheckout request;

    @BeforeEach
    void setUp() {
        idCliente = UUID.randomUUID();
        idTienda = UUID.randomUUID();
        request = new SolicitudCheckout();
    }

    @Test
    void debeCrearOrdenSiCarritoTieneItems() {
        Carrito carrito = new Carrito();
        ArticuloCarrito articulo = new ArticuloCarrito();
        articulo.setIdProducto(UUID.randomUUID());
        articulo.setCantidad(2);
        articulo.setPrecioUnitario(new BigDecimal("50.00"));
        articulo.setMoneda("USD");
        carrito.setArticulos(List.of(articulo));

        when(servicioCarrito.getOrCreateCart(idCliente, idTienda)).thenReturn(carrito);

        Usuario usuario = new Usuario();
        usuario.setCorreo("test@test.com");
        usuario.setNombre("Test");
        usuario.setApellido("User");
        
        when(repositorioUsuario.findById(idCliente)).thenReturn(Optional.of(usuario));
        when(repositorioOrden.save(any(Orden.class))).thenAnswer(i -> i.getArguments()[0]);

        RespuestaOrden respuesta = casoUsoOrden.createOrderFromCart(idCliente, idTienda, request);

        assertNotNull(respuesta);
        assertEquals(idCliente, respuesta.getIdCliente());
        verify(repositorioOrden).save(any(Orden.class));
        verify(servicioCarrito).clearCart(idCliente);
        verify(eventPublisher).publish(any(List.class));
    }

    @Test
    void debeLanzarExcepcionSiCarritoEstaVacio() {
        Carrito carritoVacio = new Carrito();
        when(servicioCarrito.getOrCreateCart(idCliente, idTienda)).thenReturn(carritoVacio);

        assertThrows(ExcepcionOperacionInvalida.class, () -> {
            casoUsoOrden.createOrderFromCart(idCliente, idTienda, request);
        });

        verify(repositorioOrden, never()).save(any());
    }

    @Test
    void debeLanzarExcepcionSiUsuarioNoExiste() {
        Carrito carrito = new Carrito();
        ArticuloCarrito articulo = new ArticuloCarrito();
        carrito.setArticulos(List.of(articulo));

        when(servicioCarrito.getOrCreateCart(idCliente, idTienda)).thenReturn(carrito);
        when(repositorioUsuario.findById(idCliente)).thenReturn(Optional.empty());

        assertThrows(ExcepcionEntidadNoEncontrada.class, () -> {
            casoUsoOrden.createOrderFromCart(idCliente, idTienda, request);
        });
    }
}
