package com.ecommerce.modulos.inquilino.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SuscripcionTest {

    private Suscripcion suscripcion;

    @BeforeEach
    void setUp() {
        suscripcion = new Suscripcion();
    }

    @Test
    void isActiveDebeSerVerdaderoCuandoEstaActivaYSinFechaDeFin() {
        // Arrange
        suscripcion.setEstado("ACTIVE");
        suscripcion.setFechaFin(null);

        // Act & Assert
        assertTrue(suscripcion.isActive());
    }

    @Test
    void isActiveDebeSerVerdaderoCuandoEstaActivaYLaFechaDeFinEsFutura() {
        // Arrange
        suscripcion.setEstado("ACTIVE");
        suscripcion.setFechaFin(LocalDateTime.now().plusDays(1));

        // Act & Assert
        assertTrue(suscripcion.isActive());
    }

    @Test
    void isActiveDebeSerFalsoCuandoEstaActivaPeroLaFechaDeFinYaPaso() {
        // Arrange
        suscripcion.setEstado("ACTIVE");
        suscripcion.setFechaFin(LocalDateTime.now().minusDays(1));

        // Act & Assert
        assertFalse(suscripcion.isActive());
    }

    @Test
    void isActiveDebeSerFalsoCuandoElEstadoNoEsActive() {
        // Arrange
        suscripcion.setEstado("CANCELLED");
        suscripcion.setFechaFin(null);

        // Act & Assert
        assertFalse(suscripcion.isActive());
    }

    @Test
    void cancelDebeCambiarElEstadoACancelledYFijarLaFechaDeFin() {
        // Act
        suscripcion.cancel();

        // Assert
        assertEquals("CANCELLED", suscripcion.getEstado());
        assertNotNull(suscripcion.getFechaFin());
        assertFalse(suscripcion.getFechaFin().isAfter(LocalDateTime.now()));
    }

    @Test
    void unaSuscripcionCanceladaNoDebeEstarActiva() {
        // Act
        suscripcion.cancel();

        // Assert
        assertFalse(suscripcion.isActive());
    }
}
