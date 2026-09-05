package com.ecommerce.modulos.catalogo.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CategoriaTest {

    @Test
    void debeSerRaizCuandoNoTieneIdPadre() {
        Categoria categoria = new Categoria();
        categoria.setIdPadre(null);

        assertTrue(categoria.isRoot());
    }

    @Test
    void noDebeSerRaizCuandoTieneIdPadre() {
        Categoria categoria = new Categoria();
        categoria.setIdPadre(UUID.randomUUID());

        assertFalse(categoria.isRoot());
    }
}
