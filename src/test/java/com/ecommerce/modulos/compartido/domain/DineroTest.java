package com.ecommerce.modulos.compartido.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class DineroTest {

    @Test
    void ofCreaDineroConMontoRedondeadoAdosDecimales() {
        Dinero dinero = Dinero.of(new BigDecimal("10.005"), "usd");

        assertEquals(new BigDecimal("10.01"), dinero.getMonto());
        assertEquals("USD", dinero.getMoneda());
    }

    @Test
    void ofLanzaExcepcionSiMontoEsNulo() {
        assertThrows(IllegalArgumentException.class, () -> Dinero.of(null, "USD"));
    }

    @Test
    void ofLanzaExcepcionSiMonedaEsNula() {
        assertThrows(IllegalArgumentException.class, () -> Dinero.of(BigDecimal.TEN, null));
    }

    @Test
    void ofLanzaExcepcionSiMonedaEsBlank() {
        assertThrows(IllegalArgumentException.class, () -> Dinero.of(BigDecimal.TEN, "  "));
    }

    @Test
    void ofLanzaExcepcionSiMonedaNoEsUnCodigoIso4217Valido() {
        assertThrows(IllegalArgumentException.class, () -> Dinero.of(BigDecimal.TEN, "XXX_NO_EXISTE"));
    }

    @Test
    void usdCreaDineroEnDolares() {
        Dinero dinero = Dinero.usd(BigDecimal.TEN);

        assertEquals("USD", dinero.getMoneda());
        assertEquals(new BigDecimal("10.00"), dinero.getMonto());
    }

    @Test
    void zeroCreaDineroConMontoCero() {
        Dinero dinero = Dinero.zero("USD");

        assertTrue(dinero.isZero());
    }

    @Test
    void addSumaDosMontosDeLaMismaMoneda() {
        Dinero a = Dinero.usd(new BigDecimal("10.00"));
        Dinero b = Dinero.usd(new BigDecimal("5.50"));

        Dinero resultado = a.add(b);

        assertEquals(new BigDecimal("15.50"), resultado.getMonto());
    }

    @Test
    void addLanzaExcepcionSiLasMonedasSonDistintas() {
        Dinero enDolares = Dinero.usd(BigDecimal.TEN);
        Dinero enEuros = Dinero.of(BigDecimal.TEN, "EUR");

        assertThrows(IllegalArgumentException.class, () -> enDolares.add(enEuros));
    }

    @Test
    void subtractRestaDosMontosDeLaMismaMoneda() {
        Dinero a = Dinero.usd(new BigDecimal("10.00"));
        Dinero b = Dinero.usd(new BigDecimal("3.00"));

        Dinero resultado = a.subtract(b);

        assertEquals(new BigDecimal("7.00"), resultado.getMonto());
    }

    @Test
    void subtractLanzaExcepcionSiLasMonedasSonDistintas() {
        Dinero enDolares = Dinero.usd(BigDecimal.TEN);
        Dinero enEuros = Dinero.of(BigDecimal.TEN, "EUR");

        assertThrows(IllegalArgumentException.class, () -> enDolares.subtract(enEuros));
    }

    @Test
    void multiplyPorEnteroMultiplicaElMonto() {
        Dinero dinero = Dinero.usd(new BigDecimal("10.00"));

        Dinero resultado = dinero.multiply(3);

        assertEquals(new BigDecimal("30.00"), resultado.getMonto());
    }

    @Test
    void multiplyPorBigDecimalMultiplicaElMonto() {
        Dinero dinero = Dinero.usd(new BigDecimal("10.00"));

        Dinero resultado = dinero.multiply(new BigDecimal("1.5"));

        assertEquals(new BigDecimal("15.00"), resultado.getMonto());
    }

    @Test
    void porcentajeCalculaElPorcentajeDelMonto() {
        Dinero dinero = Dinero.usd(new BigDecimal("200.00"));

        Dinero resultado = dinero.porcentaje(new BigDecimal("10"));

        assertEquals(new BigDecimal("20.00"), resultado.getMonto());
    }

    @Test
    void isGreaterThanDevuelveTrueSiElMontoEsMayor() {
        Dinero mayor = Dinero.usd(new BigDecimal("20.00"));
        Dinero menor = Dinero.usd(new BigDecimal("10.00"));

        assertTrue(mayor.isGreaterThan(menor));
        assertFalse(menor.isGreaterThan(mayor));
    }

    @Test
    void isGreaterThanLanzaExcepcionSiLasMonedasSonDistintas() {
        Dinero enDolares = Dinero.usd(BigDecimal.TEN);
        Dinero enEuros = Dinero.of(BigDecimal.TEN, "EUR");

        assertThrows(IllegalArgumentException.class, () -> enDolares.isGreaterThan(enEuros));
    }

    @Test
    void isZeroDevuelveFalseSiElMontoNoEsCero() {
        Dinero dinero = Dinero.usd(BigDecimal.ONE);

        assertFalse(dinero.isZero());
    }

    @Test
    void toStringFormateaMonedaYMonto() {
        Dinero dinero = Dinero.usd(new BigDecimal("10.00"));

        assertEquals("USD 10.00", dinero.toString());
    }
}
