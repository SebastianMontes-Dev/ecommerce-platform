package com.ecommerce.modulos.compartido.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class EnlaceCortoTest {

    @Test
    void ofCreaEnlaceCortoConSlugValido() {
        EnlaceCorto enlace = EnlaceCorto.of("zapatillas-running-2024");

        assertEquals("zapatillas-running-2024", enlace.getValue());
    }

    @Test
    void ofLanzaExcepcionSiElValorEsNulo() {
        assertThrows(IllegalArgumentException.class, () -> EnlaceCorto.of(null));
    }

    @Test
    void ofLanzaExcepcionSiElValorEsBlank() {
        assertThrows(IllegalArgumentException.class, () -> EnlaceCorto.of("   "));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Con-Mayusculas", "con espacios", "con--guion-doble-al-inicio", "-empieza-con-guion", "termina-con-guion-", "con_guion_bajo", "con/slash"})
    void ofLanzaExcepcionSiElFormatoNoEsUnSlugValido(String slugInvalido) {
        assertThrows(IllegalArgumentException.class, () -> EnlaceCorto.of(slugInvalido));
    }

    @Test
    void fromTextConvierteTextoLibreEnSlugValido() {
        EnlaceCorto enlace = EnlaceCorto.fromText("  Zapatillas Running  2024!!  ");

        assertEquals("zapatillas-running-2024", enlace.getValue());
    }

    @Test
    void fromTextEliminaCaracteresEspecialesYNormalizaGuiones() {
        EnlaceCorto enlace = EnlaceCorto.fromText("Café & Té -- Edición Limitada");

        assertEquals("caf-t-edicin-limitada", enlace.getValue());
    }

    @Test
    void fromTextLanzaExcepcionSiElTextoEsNulo() {
        assertThrows(IllegalArgumentException.class, () -> EnlaceCorto.fromText(null));
    }

    @Test
    void fromTextLanzaExcepcionSiElTextoEsBlank() {
        assertThrows(IllegalArgumentException.class, () -> EnlaceCorto.fromText("   "));
    }

    @Test
    void toStringDevuelveElValorDelSlug() {
        EnlaceCorto enlace = EnlaceCorto.of("mi-slug");

        assertEquals("mi-slug", enlace.toString());
    }
}
