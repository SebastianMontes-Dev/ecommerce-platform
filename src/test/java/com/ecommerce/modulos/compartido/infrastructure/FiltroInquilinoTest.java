package com.ecommerce.modulos.compartido.infrastructure;

import com.ecommerce.modulos.identidad.application.DetallesUsuarioPersonalizado;
import com.ecommerce.modulos.identidad.domain.Usuario;
import com.ecommerce.modulos.inquilino.application.ServicioResolutorInquilino;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FiltroInquilinoTest {

    @Mock
    private ServicioResolutorInquilino servicioResolutorInquilino;

    @InjectMocks
    private FiltroInquilino filtroInquilino;

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
        ContextoInquilino.clear();
    }

    @Test
    void ignoraElHeaderSpoofeadoCuandoElUsuarioTieneTiendaPropia() throws Exception {
        UUID idUsuario = UUID.randomUUID();
        UUID idTiendaPropia = UUID.randomUUID();
        UUID idTiendaSpoofeada = UUID.randomUUID();

        Usuario usuario = new Usuario();
        usuario.setId(idUsuario);
        usuario.setCorreo("vendedor@test.com");
        DetallesUsuarioPersonalizado principal = new DetallesUsuarioPersonalizado(usuario);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        when(servicioResolutorInquilino.resolverTiendaPropia(idUsuario)).thenReturn(Optional.of(idTiendaPropia));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Inquilino-ID", idTiendaSpoofeada.toString());
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        final UUID[] idTiendaDuranteRequest = new UUID[1];
        final UUID[] idTiendaPropiaDuranteRequest = new UUID[1];
        doAnswer(invocation -> {
            idTiendaDuranteRequest[0] = ContextoInquilino.getIdTienda();
            idTiendaPropiaDuranteRequest[0] = ContextoInquilino.getIdTiendaPropia();
            return null;
        }).when(chain).doFilter(request, response);

        filtroInquilino.doFilter(request, response, chain);

        assertEquals(idTiendaPropia, idTiendaDuranteRequest[0], "el header spoofeado no debe ganar sobre la tienda propia");
        assertEquals(idTiendaPropia, idTiendaPropiaDuranteRequest[0]);
    }

    @Test
    void usaElHeaderCuandoElUsuarioNoTieneTiendaPropia() throws Exception {
        UUID idUsuario = UUID.randomUUID();
        UUID idTiendaHeader = UUID.randomUUID();

        Usuario usuario = new Usuario();
        usuario.setId(idUsuario);
        usuario.setCorreo("cliente@test.com");
        DetallesUsuarioPersonalizado principal = new DetallesUsuarioPersonalizado(usuario);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        when(servicioResolutorInquilino.resolverTiendaPropia(idUsuario)).thenReturn(Optional.empty());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Inquilino-ID", idTiendaHeader.toString());
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        final UUID[] idTiendaDuranteRequest = new UUID[1];
        doAnswer(invocation -> {
            idTiendaDuranteRequest[0] = ContextoInquilino.getIdTienda();
            return null;
        }).when(chain).doFilter(request, response);

        filtroInquilino.doFilter(request, response, chain);

        assertEquals(idTiendaHeader, idTiendaDuranteRequest[0]);
    }
}
