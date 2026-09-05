package com.ecommerce.modulos.identidad.application;

import com.ecommerce.modulos.identidad.application.dto.RespuestaAutenticacion;
import com.ecommerce.modulos.identidad.application.dto.SolicitudActualizarToken;
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

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CasoUsoActualizarTokenTest {

    @Mock
    private RepositorioTokenActualizacion repositorioTokenActualizacion;
    @Mock
    private ProveedorTokenJwt proveedorTokenJwt;
    @Mock
    private RepositorioUsuario repositorioUsuario;

    @InjectMocks
    private CasoUsoActualizarToken casoUsoActualizarToken;

    private SolicitudActualizarToken request;
    private UUID idUsuario;

    @BeforeEach
    void setUp() {
        request = SolicitudActualizarToken.builder()
                .tokenActualizacion("refresh-token-valido")
                .build();
        idUsuario = UUID.randomUUID();
    }

    @Test
    void debeRenovarTokensSiElRefreshTokenEsValido() {
        TokenActualizacion storedToken = new TokenActualizacion(
                "refresh-token-valido", idUsuario, LocalDateTime.now().plusDays(1));
        Usuario usuario = new Usuario("usuario@correo.com", "hash", "Nombre", "Apellido");
        usuario.setId(idUsuario);

        when(repositorioTokenActualizacion.findByToken("refresh-token-valido"))
                .thenReturn(Optional.of(storedToken));
        when(repositorioUsuario.findById(idUsuario)).thenReturn(Optional.of(usuario));
        when(proveedorTokenJwt.generateAccessToken(any(DetallesUsuarioPersonalizado.class)))
                .thenReturn("nuevo-access-token");
        when(proveedorTokenJwt.generateRefreshToken()).thenReturn("nuevo-refresh-token");
        when(proveedorTokenJwt.getAccessTokenExpiration()).thenReturn(3_600_000L);
        when(proveedorTokenJwt.getRefreshTokenExpiration()).thenReturn(604_800_000L);

        RespuestaAutenticacion respuesta = casoUsoActualizarToken.execute(request);

        assertNotNull(respuesta);
        assertEquals("nuevo-access-token", respuesta.getAccessToken());
        assertEquals("nuevo-refresh-token", respuesta.getTokenActualizacion());
        assertEquals(3_600L, respuesta.getExpiresIn());
        assertTrue(storedToken.isRevoked());
        verify(repositorioTokenActualizacion).save(storedToken);
        verify(repositorioTokenActualizacion).save(argThat(t ->
                t.getToken().equals("nuevo-refresh-token") && t.getUserId().equals(idUsuario)));
    }

    @Test
    void debeLanzarExcepcionSiElRefreshTokenNoExiste() {
        when(repositorioTokenActualizacion.findByToken("refresh-token-valido"))
                .thenReturn(Optional.empty());

        ExcepcionNoAutorizado excepcion = assertThrows(ExcepcionNoAutorizado.class,
                () -> casoUsoActualizarToken.execute(request));

        assertEquals("Invalid refresh token", excepcion.getMessage());
        verify(repositorioTokenActualizacion, never()).save(any());
    }

    @Test
    void debeLanzarExcepcionSiElRefreshTokenEstaExpirado() {
        TokenActualizacion tokenExpirado = new TokenActualizacion(
                "refresh-token-valido", idUsuario, LocalDateTime.now().minusMinutes(1));

        when(repositorioTokenActualizacion.findByToken("refresh-token-valido"))
                .thenReturn(Optional.of(tokenExpirado));

        ExcepcionNoAutorizado excepcion = assertThrows(ExcepcionNoAutorizado.class,
                () -> casoUsoActualizarToken.execute(request));

        assertEquals("Token has expired", excepcion.getMessage());
        verify(repositorioTokenActualizacion, never()).save(any());
    }

    @Test
    void debeLanzarExcepcionSiElRefreshTokenYaFueRevocado() {
        TokenActualizacion tokenRevocado = new TokenActualizacion(
                "refresh-token-valido", idUsuario, LocalDateTime.now().plusDays(1));
        tokenRevocado.revoke();

        when(repositorioTokenActualizacion.findByToken("refresh-token-valido"))
                .thenReturn(Optional.of(tokenRevocado));

        ExcepcionNoAutorizado excepcion = assertThrows(ExcepcionNoAutorizado.class,
                () -> casoUsoActualizarToken.execute(request));

        assertEquals("Token has expired", excepcion.getMessage());
        verify(repositorioTokenActualizacion, never()).save(any());
    }

    @Test
    void debeLanzarExcepcionSiElUsuarioDelTokenYaNoExiste() {
        TokenActualizacion storedToken = new TokenActualizacion(
                "refresh-token-valido", idUsuario, LocalDateTime.now().plusDays(1));

        when(repositorioTokenActualizacion.findByToken("refresh-token-valido"))
                .thenReturn(Optional.of(storedToken));
        when(repositorioUsuario.findById(idUsuario)).thenReturn(Optional.empty());

        ExcepcionNoAutorizado excepcion = assertThrows(ExcepcionNoAutorizado.class,
                () -> casoUsoActualizarToken.execute(request));

        assertEquals("Usuario not found", excepcion.getMessage());
        // el token viejo ya fue revocado y guardado antes de fallar la busqueda de usuario
        verify(repositorioTokenActualizacion).save(storedToken);
        verify(repositorioTokenActualizacion, times(1)).save(any());
    }
}
