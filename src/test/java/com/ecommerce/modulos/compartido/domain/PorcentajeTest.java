package com.ecommerce.modulos.compartido.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class PorcentajeTest {

    @Test
    void ofBigDecimalCreaPorcentajeConDosDecimales() {
        Porcentaje porcentaje = Porcentaje.of(new BigDecimal("15.5"));

        assertEquals(new BigDecimal("15.50"), porcentaje.getValue());
    }

    @Test
    void ofDoubleCreaPorcentajeValido() {
        Porcentaje porcentaje = Porcentaje.of(50.0);

        assertEquals(new BigDecimal("50.00"), porcentaje.getValue());
    }

    @Test
    void ofPermiteElLimiteInferiorCero() {
        Porcentaje porcentaje = Porcentaje.of(BigDecimal.ZERO);

        assertEquals(new BigDecimal("0.00"), porcentaje.getValue());
    }

    @Test
    void ofPermiteElLimiteSuperiorCien() {
        Porcentaje porcentaje = Porcentaje.of(BigDecimal.valueOf(100));

        assertEquals(new BigDecimal("100.00"), porcentaje.getValue());
    }

    @Test
    void ofLanzaExcepcionSiElValorEsNulo() {
        assertThrows(IllegalArgumentException.class, () -> Porcentaje.of((BigDecimal) null));
    }

    @Test
    void ofLanzaExcepcionSiElValorEsNegativo() {
        assertThrows(IllegalArgumentException.class, () -> Porcentaje.of(new BigDecimal("-0.01")));
    }

    @Test
    void ofLanzaExcepcionSiElValorSuperaCien() {
        assertThrows(IllegalArgumentException.class, () -> Porcentaje.of(new BigDecimal("100.01")));
    }

    @Test
    void zeroDevuelvePorcentajeCero() {
        assertEquals(new BigDecimal("0.00"), Porcentaje.zero().getValue());
    }

    @Test
    void applyToAplicaElPorcentajeAUnDinero() {
        Porcentaje porcentaje = Porcentaje.of(10.0);
        Dinero dinero = Dinero.usd(new BigDecimal("200.00"));

        Dinero resultado = porcentaje.applyTo(dinero);

        assertEquals(new BigDecimal("20.00"), resultado.getMonto());
    }

    @Test
    void toStringAgregaElSimboloDePorcentaje() {
        Porcentaje porcentaje = Porcentaje.of(25.0);

        assertEquals("25.00%", porcentaje.toString());
    }
}
