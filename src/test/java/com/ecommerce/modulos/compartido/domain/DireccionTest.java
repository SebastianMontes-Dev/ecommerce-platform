package com.ecommerce.modulos.compartido.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DireccionTest {

    @Test
    void ofSinInfoAdicionalCreaDireccionValida() {
        Direccion direccion = Direccion.of("Av. Siempre Viva 742", "Springfield", "IL", "62704", "USA");

        assertEquals("Av. Siempre Viva 742", direccion.getStreet());
        assertEquals("Springfield", direccion.getCity());
        assertEquals("IL", direccion.getState());
        assertEquals("62704", direccion.getCodigoPostal());
        assertEquals("USA", direccion.getCountry());
        assertNull(direccion.getAdditionalInfo());
    }

    @Test
    void ofConInfoAdicionalLaAsigna() {
        Direccion direccion = Direccion.of("Calle Falsa 123", "Springfield", "IL", "62704", "USA", "Depto 4B");

        assertEquals("Depto 4B", direccion.getAdditionalInfo());
    }

    @Test
    void ofLanzaExcepcionSiStreetEsNulo() {
        assertThrows(IllegalArgumentException.class, () -> Direccion.of(null, "Springfield", "IL", "62704", "USA"));
    }

    @Test
    void ofLanzaExcepcionSiStreetEsBlank() {
        assertThrows(IllegalArgumentException.class, () -> Direccion.of("   ", "Springfield", "IL", "62704", "USA"));
    }

    @Test
    void ofLanzaExcepcionSiCityEsNulo() {
        assertThrows(IllegalArgumentException.class, () -> Direccion.of("Calle Falsa 123", null, "IL", "62704", "USA"));
    }

    @Test
    void ofLanzaExcepcionSiCountryEsNulo() {
        assertThrows(IllegalArgumentException.class, () -> Direccion.of("Calle Falsa 123", "Springfield", "IL", "62704", null));
    }

    @Test
    void ofPermiteStateYCodigoPostalNulos() {
        Direccion direccion = Direccion.of("Calle Falsa 123", "Springfield", null, null, "USA");

        assertNull(direccion.getState());
        assertNull(direccion.getCodigoPostal());
    }

    @Test
    void toStringIncluyeStateYCodigoPostalCuandoEstanPresentes() {
        Direccion direccion = Direccion.of("Calle Falsa 123", "Springfield", "IL", "62704", "USA");

        assertEquals("Calle Falsa 123, Springfield, IL 62704, USA", direccion.toString());
    }

    @Test
    void toStringOmiteStateYCodigoPostalCuandoSonNulos() {
        Direccion direccion = Direccion.of("Calle Falsa 123", "Springfield", null, null, "USA");

        assertEquals("Calle Falsa 123, Springfield, USA", direccion.toString());
    }
}
