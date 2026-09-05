package com.ecommerce.modulos.notificacion.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class NotificacionTest {

    @Test
    void debeQuedarEnEstadoPendienteAlCrearse() {
        Notificacion notificacion = new Notificacion();

        assertEquals("PENDING", notificacion.getEstado());
        assertNull(notificacion.getEnviadoEn());
        assertNull(notificacion.getMensajeError());
    }

    @Test
    void markAsSentDebeCambiarEstadoASentYRegistrarFechaDeEnvio() {
        Notificacion notificacion = new Notificacion();

        notificacion.markAsSent();

        assertEquals("SENT", notificacion.getEstado());
        assertNotNull(notificacion.getEnviadoEn());
        assertTrue(notificacion.getEnviadoEn().isBefore(LocalDateTime.now().plusSeconds(1)));
    }

    @Test
    void markAsFailedDebeCambiarEstadoAFailedYRegistrarMensajeDeError() {
        Notificacion notificacion = new Notificacion();

        notificacion.markAsFailed("SMTP timeout");

        assertEquals("FAILED", notificacion.getEstado());
        assertEquals("SMTP timeout", notificacion.getMensajeError());
        assertNull(notificacion.getEnviadoEn());
    }

    @Test
    void markAsFailedNoDebeSobrescribirLaFechaDeEnvioDeUnEnvioPrevio() {
        Notificacion notificacion = new Notificacion();
        notificacion.markAsSent();
        LocalDateTime enviadoEn = notificacion.getEnviadoEn();

        notificacion.markAsFailed("reintento fallido");

        assertEquals("FAILED", notificacion.getEstado());
        assertEquals(enviadoEn, notificacion.getEnviadoEn());
    }
}
