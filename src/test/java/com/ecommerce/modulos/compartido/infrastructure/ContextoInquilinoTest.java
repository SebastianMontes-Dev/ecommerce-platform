package com.ecommerce.modulos.compartido.infrastructure;

import com.ecommerce.modulos.compartido.domain.ExcepcionNoAutorizado;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ContextoInquilinoTest {

    @AfterEach
    void limpiar() {
        ContextoInquilino.clear();
    }

    @Test
    void getIdTiendaPropiaDevuelveElValorConfiable() {
        UUID idTienda = UUID.randomUUID();
        ContextoInquilino.setIdTiendaPropia(idTienda);

        assertEquals(idTienda, ContextoInquilino.getIdTiendaPropia());
    }

    @Test
    void getIdTiendaPropiaLanzaExcepcionSiNoHayTiendaPropia() {
        assertThrows(ExcepcionNoAutorizado.class, ContextoInquilino::getIdTiendaPropia);
    }

    @Test
    void clearLimpiaAmbosValores() {
        ContextoInquilino.setIdTienda(UUID.randomUUID());
        ContextoInquilino.setIdTiendaPropia(UUID.randomUUID());

        ContextoInquilino.clear();

        assertNull(ContextoInquilino.getIdTienda());
        assertThrows(ExcepcionNoAutorizado.class, ContextoInquilino::getIdTiendaPropia);
    }
}
