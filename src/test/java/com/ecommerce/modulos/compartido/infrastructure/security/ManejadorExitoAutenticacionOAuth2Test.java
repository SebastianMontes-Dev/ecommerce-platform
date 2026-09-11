package com.ecommerce.modulos.compartido.infrastructure.security;

import com.ecommerce.modulos.identidad.domain.RepositorioTokenActualizacion;
import com.ecommerce.modulos.identidad.domain.RepositorioUsuario;
import com.ecommerce.modulos.identidad.domain.TokenActualizacion;
import com.ecommerce.modulos.identidad.domain.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManejadorExitoAutenticacionOAuth2Test {

    @Mock private RepositorioUsuario repositorioUsuario;
    @Mock private RepositorioTokenActualizacion repositorioTokenActualizacion;
    @Mock private ProveedorTokenJwt proveedorTokenJwt;

    private ManejadorExitoAutenticacionOAuth2 manejador;

    @BeforeEach
    void setUp() {
        manejador = new ManejadorExitoAutenticacionOAuth2(
                repositorioUsuario, repositorioTokenActualizacion, proveedorTokenJwt,
                "http://localhost:3000/oauth2/callback");
    }

    private static OAuth2User principalConCorreo(String correo) {
        return new DefaultOAuth2User(List.of(), Map.of("email", correo, "sub", "123"), "email");
    }

    @Test
    void emiteJwtYRedirigeAlFrontendConElTokenEnElFragmento() throws Exception {
        Usuario usuario = new Usuario("juan@test.com", "", "Juan", "Perez");
        usuario.setId(UUID.randomUUID());
        when(repositorioUsuario.findByCorreo("juan@test.com")).thenReturn(Optional.of(usuario));
        when(proveedorTokenJwt.generateAccessToken(any())).thenReturn("un.jwt.valido");
        when(proveedorTokenJwt.generateRefreshToken()).thenReturn("un-refresh-token");
        when(proveedorTokenJwt.getAccessTokenExpiration()).thenReturn(900000L);
        when(proveedorTokenJwt.getRefreshTokenExpiration()).thenReturn(604800000L);

        MockHttpServletResponse response = new MockHttpServletResponse();
        manejador.onAuthenticationSuccess(new MockHttpServletRequest(), response,
                new UsernamePasswordAuthenticationToken(principalConCorreo("juan@test.com"), null, List.of()));

        assertEquals(302, response.getStatus());
        String destino = response.getRedirectedUrl();
        assertNotNull(destino);
        assertTrue(destino.startsWith("http://localhost:3000/oauth2/callback#"));
        assertTrue(destino.contains("access_token=un.jwt.valido"));
        assertTrue(destino.contains("refresh_token=un-refresh-token"));
        assertTrue(destino.contains("expires_in=900"));

        ArgumentCaptor<TokenActualizacion> captor = ArgumentCaptor.forClass(TokenActualizacion.class);
        verify(repositorioTokenActualizacion).save(captor.capture());
        assertEquals(usuario.getId(), captor.getValue().getUserId());
    }

    @Test
    void fallaSiElUsuarioNoExisteTodavia() {
        when(repositorioUsuario.findByCorreo("fantasma@test.com")).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> manejador.onAuthenticationSuccess(
                new MockHttpServletRequest(), new MockHttpServletResponse(),
                new UsernamePasswordAuthenticationToken(principalConCorreo("fantasma@test.com"), null, List.of())));

        verifyNoInteractions(repositorioTokenActualizacion);
    }
}
