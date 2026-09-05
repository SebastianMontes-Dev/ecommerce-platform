package com.ecommerce.modulos.identidad.application;

import com.ecommerce.modulos.identidad.domain.RepositorioUsuario;
import com.ecommerce.modulos.identidad.domain.RolUsuario;
import com.ecommerce.modulos.identidad.domain.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioDetallesUsuarioPersonalizadoTest {

    @Mock
    private RepositorioUsuario repositorioUsuario;

    @InjectMocks
    private ServicioDetallesUsuarioPersonalizado servicioDetallesUsuarioPersonalizado;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = new Usuario("existente@correo.com", "hash-secreto", "Nombre", "Apellido");
        usuario.addRole(RolUsuario.CUSTOMER);
    }

    @Test
    void debeCargarUsuarioSiElCorreoExiste() {
        when(repositorioUsuario.findByCorreo("existente@correo.com")).thenReturn(Optional.of(usuario));

        UserDetails userDetails = servicioDetallesUsuarioPersonalizado.loadUserByUsername("existente@correo.com");

        assertNotNull(userDetails);
        assertEquals("existente@correo.com", userDetails.getUsername());
        assertEquals("hash-secreto", userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_CUSTOMER"::equals));
    }

    @Test
    void debeNormalizarCorreoAMinusculasYSinEspaciosAlBuscar() {
        // el repositorio solo tiene el stub para el correo ya normalizado:
        // si loadUserByUsername no normalizara, la busqueda no calzaria y lanzaria UsernameNotFoundException
        when(repositorioUsuario.findByCorreo("existente@correo.com")).thenReturn(Optional.of(usuario));

        UserDetails userDetails = servicioDetallesUsuarioPersonalizado.loadUserByUsername("  Existente@Correo.com  ");

        assertEquals("existente@correo.com", userDetails.getUsername());
    }

    @Test
    void debeLanzarUsernameNotFoundExceptionSiElCorreoNoExiste() {
        when(repositorioUsuario.findByCorreo("inexistente@correo.com")).thenReturn(Optional.empty());

        UsernameNotFoundException excepcion = assertThrows(UsernameNotFoundException.class,
                () -> servicioDetallesUsuarioPersonalizado.loadUserByUsername("inexistente@correo.com"));

        assertTrue(excepcion.getMessage().contains("inexistente@correo.com"));
    }
}
