package com.ecommerce.modulos.identidad.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UsuarioTest {

    @Test
    void debeRetornarNombreYApellidoConcatenadosCuandoAmbosExisten() {
        Usuario usuario = new Usuario("test@correo.com", "hash", "Ana", "Perez");

        assertEquals("Ana Perez", usuario.getNombreCompleto());
    }

    @Test
    void debeRetornarSoloNombreCuandoApellidoEsNulo() {
        Usuario usuario = new Usuario("test@correo.com", "hash", "Ana", null);

        assertEquals("Ana", usuario.getNombreCompleto());
    }

    @Test
    void debeRetornarSoloApellidoCuandoNombreEsNulo() {
        Usuario usuario = new Usuario("test@correo.com", "hash", null, "Perez");

        assertEquals("Perez", usuario.getNombreCompleto());
    }

    @Test
    void debeRetornarCorreoComoFallbackCuandoNombreYApellidoSonNulos() {
        Usuario usuario = new Usuario("test@correo.com", "hash", null, null);

        assertEquals("test@correo.com", usuario.getNombreCompleto());
    }

    @Test
    void debeEstarHabilitadoYSinEmailVerificadoAlCrearUsuario() {
        Usuario usuario = new Usuario("test@correo.com", "hash", "Ana", "Perez");

        assertTrue(usuario.isEnabled());
        assertFalse(usuario.isEmailVerified());
        assertTrue(usuario.getRoles().isEmpty());
    }

    @Test
    void debeQuedarDeshabilitadoDespuesDeLlamarDisable() {
        Usuario usuario = new Usuario("test@correo.com", "hash", "Ana", "Perez");

        usuario.disable();

        assertFalse(usuario.isEnabled());
    }

    @Test
    void debeQuedarHabilitadoDespuesDeLlamarEnableSobreUnoDeshabilitado() {
        Usuario usuario = new Usuario("test@correo.com", "hash", "Ana", "Perez");
        usuario.disable();

        usuario.enable();

        assertTrue(usuario.isEnabled());
    }

    @Test
    void debeMarcarEmailComoVerificado() {
        Usuario usuario = new Usuario("test@correo.com", "hash", "Ana", "Perez");

        usuario.verifyEmail();

        assertTrue(usuario.isEmailVerified());
    }

    @Test
    void debeCambiarElHashDeContrasena() {
        Usuario usuario = new Usuario("test@correo.com", "hash-viejo", "Ana", "Perez");

        usuario.changePassword("hash-nuevo");

        assertEquals("hash-nuevo", usuario.getHashContrasena());
    }

    @Test
    void debeAgregarYDetectarUnRol() {
        Usuario usuario = new Usuario("test@correo.com", "hash", "Ana", "Perez");

        usuario.addRole(RolUsuario.SELLER);

        assertTrue(usuario.hasRole(RolUsuario.SELLER));
        assertFalse(usuario.hasRole(RolUsuario.CUSTOMER));
    }

    @Test
    void debeIgnorarRolDuplicadoAlAgregarloDosVeces() {
        Usuario usuario = new Usuario("test@correo.com", "hash", "Ana", "Perez");

        usuario.addRole(RolUsuario.CUSTOMER);
        usuario.addRole(RolUsuario.CUSTOMER);

        assertEquals(1, usuario.getRoles().size());
    }

    @Test
    void debeRemoverUnRolExistente() {
        Usuario usuario = new Usuario("test@correo.com", "hash", "Ana", "Perez");
        usuario.addRole(RolUsuario.CUSTOMER);

        usuario.removeRole(RolUsuario.CUSTOMER);

        assertFalse(usuario.hasRole(RolUsuario.CUSTOMER));
    }

    @Test
    void debeNoLanzarExcepcionAlRemoverUnRolQueNoTiene() {
        Usuario usuario = new Usuario("test@correo.com", "hash", "Ana", "Perez");

        assertDoesNotThrow(() -> usuario.removeRole(RolUsuario.PLATFORM_ADMIN));
    }
}
