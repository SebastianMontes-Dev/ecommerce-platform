package com.ecommerce.modulos.compartido.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class CorreoTest {

    @Test
    void ofCreaCorreoConValorValido() {
        Correo correo = Correo.of("usuario@dominio.com");

        assertEquals("usuario@dominio.com", correo.getValue());
    }

    @Test
    void ofNormalizaAMinusculasYRecortaEspacios() {
        Correo correo = Correo.of("  Usuario@Dominio.COM  ".trim());

        assertEquals("usuario@dominio.com", correo.getValue());
    }

    @Test
    void ofLanzaExcepcionSiElValorEsNulo() {
        assertThrows(IllegalArgumentException.class, () -> Correo.of(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"sin-arroba.com", "@dominio.com", "usuario@", "usuario@dominio", "usuario @dominio.com", ""})
    void ofLanzaExcepcionSiElFormatoEsInvalido(String valorInvalido) {
        assertThrows(IllegalArgumentException.class, () -> Correo.of(valorInvalido));
    }

    @Test
    void toStringDevuelveElValorDelCorreo() {
        Correo correo = Correo.of("usuario@dominio.com");

        assertEquals("usuario@dominio.com", correo.toString());
    }

    @Test
    void equalsEsConsistenteEntreDosCorreosConElMismoValor() {
        Correo a = Correo.of("usuario@dominio.com");
        Correo b = Correo.of("usuario@dominio.com");

        assertEquals(a, b);
    }
}
