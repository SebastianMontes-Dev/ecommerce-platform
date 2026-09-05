package com.ecommerce.modulos.identidad.application;

import com.ecommerce.modulos.identidad.application.dto.RespuestaAutenticacion;
import com.ecommerce.modulos.identidad.application.dto.SolicitudInicioSesion;
import com.ecommerce.modulos.identidad.domain.RepositorioTokenActualizacion;
import com.ecommerce.modulos.identidad.domain.RepositorioUsuario;
import com.ecommerce.modulos.identidad.domain.TokenActualizacion;
import com.ecommerce.modulos.identidad.domain.Usuario;
import com.ecommerce.modulos.compartido.domain.ExcepcionNoAutorizado;
import com.ecommerce.modulos.compartido.infrastructure.security.ProveedorTokenJwt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CasoUsoIniciarSesionTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private ProveedorTokenJwt proveedorTokenJwt;
    @Mock
    private RepositorioTokenActualizacion repositorioTokenActualizacion;
    @Mock
    private RepositorioUsuario repositorioUsuario;
    @Mock
    private Authentication authentication;

    @InjectMocks
    private CasoUsoIniciarSesion casoUsoIniciarSesion;

    private SolicitudInicioSesion request;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        request = SolicitudInicioSesion.builder()
                .correo("Test@Correo.com")
                .contrasena("password123")
                .build();

        usuario = new Usuario("test@correo.com", "hash", "Test", "Usuario");
    }

    @Test
    void debeIniciarSesionExitosamenteYRetornarTokens() {
        DetallesUsuarioPersonalizado userDetails = new DetallesUsuarioPersonalizado(usuario);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(repositorioUsuario.findByCorreo("test@correo.com")).thenReturn(Optional.of(usuario));
        when(proveedorTokenJwt.generateAccessToken(userDetails)).thenReturn("access-token");
        when(proveedorTokenJwt.generateRefreshToken()).thenReturn("refresh-token-value");
        when(proveedorTokenJwt.getAccessTokenExpiration()).thenReturn(3_600_000L);
        when(proveedorTokenJwt.getRefreshTokenExpiration()).thenReturn(604_800_000L);

        RespuestaAutenticacion respuesta = casoUsoIniciarSesion.execute(request);

        assertNotNull(respuesta);
        assertEquals("access-token", respuesta.getAccessToken());
        assertEquals("refresh-token-value", respuesta.getTokenActualizacion());
        assertEquals(3_600L, respuesta.getExpiresIn());
        assertEquals("Bearer", respuesta.getTokenType());
        verify(repositorioTokenActualizacion).save(any(TokenActualizacion.class));
    }

    @Test
    void debeLanzarExcepcionNoAutorizadoSiLasCredencialesSonInvalidas() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        ExcepcionNoAutorizado excepcion = assertThrows(ExcepcionNoAutorizado.class,
                () -> casoUsoIniciarSesion.execute(request));

        assertEquals("Invalid correo or contrasena", excepcion.getMessage());
        verify(repositorioTokenActualizacion, never()).save(any());
    }

    @Test
    void debeLanzarExcepcionNoAutorizadoSiElUsuarioEstaDeshabilitado() {
        usuario.disable();
        DetallesUsuarioPersonalizado userDetails = new DetallesUsuarioPersonalizado(usuario);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(repositorioUsuario.findByCorreo("test@correo.com")).thenReturn(Optional.of(usuario));

        ExcepcionNoAutorizado excepcion = assertThrows(ExcepcionNoAutorizado.class,
                () -> casoUsoIniciarSesion.execute(request));

        assertEquals("Account is disabled", excepcion.getMessage());
        verify(proveedorTokenJwt, never()).generateAccessToken(any());
        verify(repositorioTokenActualizacion, never()).save(any());
    }

    @Test
    void debeLanzarUsernameNotFoundSiElUsuarioAutenticadoYaNoExisteEnElRepositorio() {
        DetallesUsuarioPersonalizado userDetails = new DetallesUsuarioPersonalizado(usuario);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(repositorioUsuario.findByCorreo("test@correo.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> casoUsoIniciarSesion.execute(request));

        verify(repositorioTokenActualizacion, never()).save(any());
    }
}
