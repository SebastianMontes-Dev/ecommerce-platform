package com.ecommerce.modulos.compartido.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class CalificacionTest {

    @Test
    void ofBigDecimalCreaCalificacionConUnDecimal() {
        Calificacion calificacion = Calificacion.of(new BigDecimal("4.25"));

        assertEquals(new BigDecimal("4.3"), calificacion.getValue());
    }

    @Test
    void ofIntCreaCalificacionValida() {
        Calificacion calificacion = Calificacion.of(3);

        assertEquals(new BigDecimal("3.0"), calificacion.getValue());
    }

    @Test
    void ofPermiteElLimiteInferiorCero() {
        Calificacion calificacion = Calificacion.of(BigDecimal.ZERO);

        assertEquals(new BigDecimal("0.0"), calificacion.getValue());
    }

    @Test
    void ofPermiteElLimiteSuperiorCinco() {
        Calificacion calificacion = Calificacion.of(BigDecimal.valueOf(5));

        assertEquals(new BigDecimal("5.0"), calificacion.getValue());
    }

    @Test
    void ofLanzaExcepcionSiElValorEsNulo() {
        assertThrows(IllegalArgumentException.class, () -> Calificacion.of((BigDecimal) null));
    }

    @Test
    void ofLanzaExcepcionSiElValorEsNegativo() {
        assertThrows(IllegalArgumentException.class, () -> Calificacion.of(new BigDecimal("-0.1")));
    }

    @Test
    void ofLanzaExcepcionSiElValorSuperaCinco() {
        assertThrows(IllegalArgumentException.class, () -> Calificacion.of(new BigDecimal("5.1")));
    }

    @Test
    void zeroDevuelveCalificacionCero() {
        assertEquals(new BigDecimal("0.0"), Calificacion.zero().getValue());
    }

    @Test
    void toStringDevuelveElValorComoTexto() {
        Calificacion calificacion = Calificacion.of(4);

        assertEquals("4.0", calificacion.toString());
    }
}
