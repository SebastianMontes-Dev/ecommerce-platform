package com.ecommerce.modulos.ordenes.domain;

import com.ecommerce.modulos.compartido.domain.ExcepcionOperacionInvalida;
import com.ecommerce.modulos.ordenes.domain.events.EventoInventarioLiberado;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OrdenTest {

    private Orden ordenEn(EstadoOrden estado) {
        Orden orden = new Orden();
        orden.setId(UUID.randomUUID());
        orden.setIdTienda(UUID.randomUUID());
        orden.setIdCliente(UUID.randomUUID());
        orden.setNumeroOrden("ORD-TEST");
        orden.setEstado(estado);
        return orden;
    }

    @Test
    @DisplayName("Una pasarela hosted confirma el pago sobre una orden PENDING: PENDING -> PAID debe ser válida")
    void permitePendingAPaid() {
        Orden orden = ordenEn(EstadoOrden.PENDING);

        assertDoesNotThrow(orden::markAsPaid);

        assertEquals(EstadoOrden.PAID, orden.getEstado());
        assertEquals(1, orden.getHistorialEstados().size());
    }

    @Test
    @DisplayName("El flujo B2B con aprobación manual sigue soportado: PENDING -> CONFIRMED -> PAID")
    void permiteConfirmadoIntermedio() {
        Orden orden = ordenEn(EstadoOrden.PENDING);

        orden.confirm();
        assertEquals(EstadoOrden.CONFIRMED, orden.getEstado());

        orden.markAsPaid();
        assertEquals(EstadoOrden.PAID, orden.getEstado());
    }

    @Test
    @DisplayName("PENDING sigue sin poder saltar directo a estados de fulfillment")
    void pendingNoSaltaAProcessing() {
        Orden orden = ordenEn(EstadoOrden.PENDING);

        assertThrows(ExcepcionOperacionInvalida.class, orden::process);
        assertThrows(ExcepcionOperacionInvalida.class, orden::ship);
        assertEquals(EstadoOrden.PENDING, orden.getEstado());
    }

    @Test
    @DisplayName("Estados terminales no admiten transición")
    void estadosTerminales() {
        Orden cancelada = ordenEn(EstadoOrden.CANCELLED);
        assertThrows(ExcepcionOperacionInvalida.class, cancelada::markAsPaid);

        Orden reembolsada = ordenEn(EstadoOrden.REFUNDED);
        assertThrows(ExcepcionOperacionInvalida.class, () -> reembolsada.process());
    }

    @Test
    @DisplayName("cancel() registra EventoInventarioLiberado con los items para reponer inventario")
    void cancelEmiteEventoDeLiberacion() {
        Orden orden = ordenEn(EstadoOrden.PENDING);

        orden.cancel("Pago expirado");

        assertTrue(orden.getDomainEvents().stream().anyMatch(e -> e instanceof EventoInventarioLiberado));
    }

    @Test
    @DisplayName("refund() también registra EventoInventarioLiberado (el stock vuelve al catálogo)")
    void refundEmiteEventoDeLiberacion() {
        Orden orden = ordenEn(EstadoOrden.DELIVERED);

        orden.refund("Producto defectuoso");

        assertEquals(EstadoOrden.REFUNDED, orden.getEstado());
        assertTrue(orden.getDomainEvents().stream().anyMatch(e -> e instanceof EventoInventarioLiberado));
    }

    @Test
    @DisplayName("El resto del ciclo de fulfillment se mantiene intacto")
    void cicloDeFulfillment() {
        Orden orden = ordenEn(EstadoOrden.PAID);

        orden.process();
        assertEquals(EstadoOrden.PROCESSING, orden.getEstado());

        orden.ship();
        assertEquals(EstadoOrden.SHIPPED, orden.getEstado());

        orden.deliver();
        assertEquals(EstadoOrden.DELIVERED, orden.getEstado());

        orden.refund("garantía");
        assertEquals(EstadoOrden.REFUNDED, orden.getEstado());
    }
}
