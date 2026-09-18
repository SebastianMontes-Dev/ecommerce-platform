package com.ecommerce.modulos.resenas.domain;

import com.ecommerce.modulos.compartido.domain.Calificacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ResenaTest {

    private Resena resena;

    @BeforeEach
    void setUp() {
        resena = new Resena();
        resena.setIdTienda(UUID.randomUUID());
        resena.setIdProducto(UUID.randomUUID());
        resena.setIdCliente(UUID.randomUUID());
        resena.setIdOrden(UUID.randomUUID());
        resena.setCalificacion(Calificacion.of(5));
        resena.setTitulo("Excelente");
        resena.setComentario("Muy buen producto");
    }

    @Test
    void debeEstarActivaPorDefectoAlCrearse() {
        Resena nueva = new Resena();

        assertTrue(nueva.isActivo());
    }

    @Test
    void hideDebeDesactivarLaResena() {
        resena.hide();

        assertFalse(resena.isActivo());
    }

    @Test
    void showDebeReactivarUnaResenaOculta() {
        resena.hide();

        resena.show();

        assertTrue(resena.isActivo());
    }

    @Test
    void showSobreUnaResenaYaActivaDebeMantenerlaActiva() {
        resena.show();

        assertTrue(resena.isActivo());
    }

    @Test
    void hideSobreUnaResenaYaOcultaDebeMantenerlaOculta() {
        resena.hide();

        resena.hide();

        assertFalse(resena.isActivo());
    }
}
