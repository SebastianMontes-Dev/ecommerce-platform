package com.ecommerce.modulos.catalogo.domain;

import com.ecommerce.modulos.catalogo.domain.eventos.EventoProductoCreado;
import com.ecommerce.modulos.compartido.domain.Dinero;
import com.ecommerce.modulos.compartido.domain.EventoDominio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProductoTest {

    private Producto producto;

    @BeforeEach
    void setUp() {
        producto = new Producto();
        producto.setIdTienda(UUID.randomUUID());
        producto.setNombre("Zapatillas");
        producto.setEnlaceCorto("zapatillas");
        producto.setPrecio(Dinero.of(new BigDecimal("200.00"), "USD"));
    }

    @Test
    void debePublicarProductoCuandoEstaEnDraft() {
        producto.setEstado(EstadoProducto.DRAFT);

        producto.publish();

        assertEquals(EstadoProducto.ACTIVE, producto.getEstado());
    }

    @Test
    void debeLanzarExcepcionAlPublicarUnProductoArchivado() {
        producto.setEstado(EstadoProducto.ARCHIVED);

        assertThrows(IllegalStateException.class, () -> producto.publish());
    }

    @Test
    void debeArchivarUnProductoActivo() {
        producto.setEstado(EstadoProducto.ACTIVE);

        producto.archive();

        assertEquals(EstadoProducto.ARCHIVED, producto.getEstado());
    }

    @Test
    void debeEstarDisponibleCuandoEstaActivoYTieneInventario() {
        producto.setEstado(EstadoProducto.ACTIVE);
        producto.setRastreoInventarioHabilitado(true);
        producto.setInventario(5);

        assertTrue(producto.isAvailable());
    }

    @Test
    void noDebeEstarDisponibleCuandoEstaActivoSinInventarioYSinReserva() {
        producto.setEstado(EstadoProducto.ACTIVE);
        producto.setRastreoInventarioHabilitado(true);
        producto.setInventario(0);
        producto.setPermitirReserva(false);

        assertFalse(producto.isAvailable());
    }

    @Test
    void debeEstarDisponibleSinInventarioCuandoSePermiteReserva() {
        producto.setEstado(EstadoProducto.ACTIVE);
        producto.setRastreoInventarioHabilitado(true);
        producto.setInventario(0);
        producto.setPermitirReserva(true);

        assertTrue(producto.isAvailable());
    }

    @Test
    void debeEstarDisponibleSinRastreoDeInventarioAunSinStock() {
        producto.setEstado(EstadoProducto.ACTIVE);
        producto.setRastreoInventarioHabilitado(false);
        producto.setInventario(0);

        assertTrue(producto.isAvailable());
    }

    @Test
    void noDebeEstarDisponibleCuandoEstaEnDraft() {
        producto.setEstado(EstadoProducto.DRAFT);
        producto.setRastreoInventarioHabilitado(true);
        producto.setInventario(10);

        assertFalse(producto.isAvailable());
    }

    @Test
    void debeReducirInventarioCuandoHayStockSuficiente() {
        producto.setRastreoInventarioHabilitado(true);
        producto.setInventario(10);

        producto.decreaseInventory(4);

        assertEquals(6, producto.getInventario());
    }

    @Test
    void debeLanzarExcepcionAlReducirInventarioInsuficienteSinPermitirReserva() {
        producto.setRastreoInventarioHabilitado(true);
        producto.setInventario(2);
        producto.setPermitirReserva(false);

        assertThrows(IllegalStateException.class, () -> producto.decreaseInventory(5));
    }

    @Test
    void debePermitirInventarioNegativoAlReducirCuandoSePermiteReserva() {
        producto.setRastreoInventarioHabilitado(true);
        producto.setInventario(2);
        producto.setPermitirReserva(true);

        producto.decreaseInventory(5);

        assertEquals(-3, producto.getInventario());
    }

    @Test
    void noDebeModificarInventarioAlReducirCuandoElRastreoEstaDeshabilitado() {
        producto.setRastreoInventarioHabilitado(false);
        producto.setInventario(2);

        producto.decreaseInventory(100);

        assertEquals(2, producto.getInventario());
    }

    @Test
    void debeIncrementarInventarioCuandoElRastreoEstaHabilitado() {
        producto.setRastreoInventarioHabilitado(true);
        producto.setInventario(3);

        producto.increaseInventory(7);

        assertEquals(10, producto.getInventario());
    }

    @Test
    void noDebeModificarInventarioAlIncrementarCuandoElRastreoEstaDeshabilitado() {
        producto.setRastreoInventarioHabilitado(false);
        producto.setInventario(3);

        producto.increaseInventory(7);

        assertEquals(3, producto.getInventario());
    }

    @Test
    void debeRegistrarEventoProductoCreadoAlMarcarComoCreado() {
        producto.markAsCreated();

        List<EventoDominio> eventos = producto.getDomainEvents();

        assertEquals(1, eventos.size());
        assertInstanceOf(EventoProductoCreado.class, eventos.get(0));
    }

    @Test
    void debeLimpiarLosEventosDeDominioAlLlamarClearDomainEvents() {
        producto.markAsCreated();

        producto.clearDomainEvents();

        assertTrue(producto.getDomainEvents().isEmpty());
    }
}
