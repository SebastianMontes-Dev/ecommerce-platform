package com.ecommerce.modulos.compartido.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CantidadTest {

    @Test
    void ofCreaCantidadConValorPositivo() {
        Cantidad cantidad = Cantidad.of(5);

        assertEquals(5, cantidad.getValue());
    }

    @Test
    void ofPermiteElLimiteInferiorUno() {
        Cantidad cantidad = Cantidad.of(1);

        assertEquals(1, cantidad.getValue());
    }

    @Test
    void ofLanzaExcepcionSiElValorEsCero() {
        assertThrows(IllegalArgumentException.class, () -> Cantidad.of(0));
    }

    @Test
    void ofLanzaExcepcionSiElValorEsNegativo() {
        assertThrows(IllegalArgumentException.class, () -> Cantidad.of(-1));
    }

    @Test
    void addSumaDosCantidades() {
        Cantidad a = Cantidad.of(3);
        Cantidad b = Cantidad.of(4);

        Cantidad resultado = a.add(b);

        assertEquals(7, resultado.getValue());
    }

    @Test
    void subtractRestaDosCantidades() {
        Cantidad a = Cantidad.of(10);
        Cantidad b = Cantidad.of(4);

        Cantidad resultado = a.subtract(b);

        assertEquals(6, resultado.getValue());
    }

    @Test
    void subtractLanzaExcepcionSiElResultadoEsMenorQueCero() {
        Cantidad a = Cantidad.of(3);
        Cantidad b = Cantidad.of(5);

        assertThrows(IllegalArgumentException.class, () -> a.subtract(b));
    }

    @Test
    void subtractLanzaExcepcionSiElResultadoEsExactamenteCero() {
        Cantidad a = Cantidad.of(5);
        Cantidad b = Cantidad.of(5);

        assertThrows(IllegalArgumentException.class, () -> a.subtract(b));
    }

    @Test
    void toStringDevuelveElValorComoTexto() {
        Cantidad cantidad = Cantidad.of(42);

        assertEquals("42", cantidad.toString());
    }
}
