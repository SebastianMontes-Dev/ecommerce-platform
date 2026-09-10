package com.ecommerce.modulos.catalogo.application;

import com.ecommerce.modulos.catalogo.domain.Producto;
import com.ecommerce.modulos.catalogo.domain.RepositorioProducto;
import com.ecommerce.modulos.compartido.domain.ExcepcionEntidadNoEncontrada;
import com.ecommerce.modulos.ordenes.domain.ArticuloOrden;
import com.ecommerce.modulos.ordenes.domain.events.EventoOrdenCancelada;
import com.ecommerce.modulos.ordenes.domain.events.EventoOrdenCreada;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManejadorEventosOrdenTest {

    @Mock
    private RepositorioProducto repositorioProducto;

    @InjectMocks
    private ManejadorEventosOrden manejadorEventosOrden;

    private UUID idTienda;

    @BeforeEach
    void setUp() {
        idTienda = UUID.randomUUID();
    }

    private ArticuloOrden crearArticulo(UUID idProducto, int cantidad) {
        ArticuloOrden articulo = new ArticuloOrden();
        articulo.setIdProducto(idProducto);
        articulo.setCantidad(cantidad);
        return articulo;
    }

    @Test
    void debeReducirElInventarioDelProductoAlProcesarLaOrden() {
        UUID idProducto = UUID.randomUUID();
        Producto producto = new Producto();
        producto.setInventario(10);
        producto.setRastreoInventarioHabilitado(true);
        when(repositorioProducto.findById(idProducto)).thenReturn(Optional.of(producto));
        when(repositorioProducto.save(any(Producto.class))).thenAnswer(i -> i.getArguments()[0]);

        EventoOrdenCreada evento = new EventoOrdenCreada(
                UUID.randomUUID(), idTienda, UUID.randomUUID(), List.of(crearArticulo(idProducto, 3)));

        manejadorEventosOrden.handle(evento);

        assertEquals(7, producto.getInventario());
        verify(repositorioProducto).save(producto);
    }

    @Test
    void debeReducirElInventarioDeCadaProductoCuandoLaOrdenTieneMultiplesItems() {
        UUID idProducto1 = UUID.randomUUID();
        UUID idProducto2 = UUID.randomUUID();
        Producto producto1 = new Producto();
        producto1.setInventario(10);
        producto1.setRastreoInventarioHabilitado(true);
        Producto producto2 = new Producto();
        producto2.setInventario(5);
        producto2.setRastreoInventarioHabilitado(true);

        when(repositorioProducto.findById(idProducto1)).thenReturn(Optional.of(producto1));
        when(repositorioProducto.findById(idProducto2)).thenReturn(Optional.of(producto2));
        when(repositorioProducto.save(any(Producto.class))).thenAnswer(i -> i.getArguments()[0]);

        EventoOrdenCreada evento = new EventoOrdenCreada(
                UUID.randomUUID(), idTienda, UUID.randomUUID(),
                List.of(crearArticulo(idProducto1, 2), crearArticulo(idProducto2, 1)));

        manejadorEventosOrden.handle(evento);

        assertEquals(8, producto1.getInventario());
        assertEquals(4, producto2.getInventario());
        verify(repositorioProducto, times(2)).save(any(Producto.class));
    }

    @Test
    void debeLanzarExcepcionSiElProductoDeLaOrdenNoExiste() {
        UUID idProducto = UUID.randomUUID();
        when(repositorioProducto.findById(idProducto)).thenReturn(Optional.empty());

        EventoOrdenCreada evento = new EventoOrdenCreada(
                UUID.randomUUID(), idTienda, UUID.randomUUID(), List.of(crearArticulo(idProducto, 1)));

        assertThrows(ExcepcionEntidadNoEncontrada.class, () -> manejadorEventosOrden.handle(evento));

        verify(repositorioProducto, never()).save(any());
    }

    @Test
    void debeReponerElInventarioAlCancelarLaOrden() {
        UUID idProducto = UUID.randomUUID();
        Producto producto = new Producto();
        producto.setInventario(7);
        producto.setRastreoInventarioHabilitado(true);
        when(repositorioProducto.findById(idProducto)).thenReturn(Optional.of(producto));
        when(repositorioProducto.save(any(Producto.class))).thenAnswer(i -> i.getArguments()[0]);

        EventoOrdenCancelada evento = new EventoOrdenCancelada(
                UUID.randomUUID(), idTienda, "Pago expirado", List.of(crearArticulo(idProducto, 3)));

        manejadorEventosOrden.handle(evento);

        assertEquals(10, producto.getInventario());
        verify(repositorioProducto).save(producto);
    }

    @Test
    void reservaYReposicionSonSimetricas() {
        UUID idProducto = UUID.randomUUID();
        Producto producto = new Producto();
        producto.setInventario(10);
        producto.setRastreoInventarioHabilitado(true);
        when(repositorioProducto.findById(idProducto)).thenReturn(Optional.of(producto));
        when(repositorioProducto.save(any(Producto.class))).thenAnswer(i -> i.getArguments()[0]);

        UUID idOrden = UUID.randomUUID();
        manejadorEventosOrden.handle(new EventoOrdenCreada(
                idOrden, idTienda, UUID.randomUUID(), List.of(crearArticulo(idProducto, 4))));
        assertEquals(6, producto.getInventario());

        manejadorEventosOrden.handle(new EventoOrdenCancelada(
                idOrden, idTienda, "Pago rechazado", List.of(crearArticulo(idProducto, 4))));
        assertEquals(10, producto.getInventario());
    }
}
