package com.ecommerce.modulos.compartido.infrastructure.security;

import com.ecommerce.modulos.identidad.application.ServicioDetallesUsuarioPersonalizado;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FiltroAutenticacionJwtTest {

    @Mock
    private ProveedorTokenJwt proveedorTokenJwt;

    @Mock
    private ServicioDetallesUsuarioPersonalizado userDetailsService;

    @InjectMocks
    private FiltroAutenticacionJwt filtroAutenticacionJwt;

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void noAutenticaSiNoHayHeaderAuthorization() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filtroAutenticacionJwt.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
        verifyNoInteractions(proveedorTokenJwt);
    }

    @Test
    void noAutenticaSiElHeaderNoTienePrefijoBearer() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic algo-cualquiera");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filtroAutenticacionJwt.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
        verifyNoInteractions(proveedorTokenJwt);
    }

    @Test
    void noAutenticaSiElTokenEsInvalido() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token-invalido");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        when(proveedorTokenJwt.validateToken("token-invalido")).thenReturn(false);

        filtroAutenticacionJwt.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
        verify(userDetailsService, never()).loadUserByUsername(any());
    }

    @Test
    void autenticaAlUsuarioSiElTokenEsValido() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token-valido");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        UserDetails userDetails = User.withUsername("cliente@test.com")
                .password("irrelevante")
                .authorities(List.of())
                .build();

        when(proveedorTokenJwt.validateToken("token-valido")).thenReturn(true);
        when(proveedorTokenJwt.getUsernameFromToken("token-valido")).thenReturn("cliente@test.com");
        when(userDetailsService.loadUserByUsername("cliente@test.com")).thenReturn(userDetails);

        filtroAutenticacionJwt.doFilter(request, response, chain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(userDetails, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        verify(chain).doFilter(request, response);
    }

    @Test
    void noPropagaLaExcepcionSiElUsuarioNoExisteYContinuaLaCadena() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token-valido");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        when(proveedorTokenJwt.validateToken("token-valido")).thenReturn(true);
        when(proveedorTokenJwt.getUsernameFromToken("token-valido")).thenReturn("fantasma@test.com");
        when(userDetailsService.loadUserByUsername("fantasma@test.com"))
                .thenThrow(new RuntimeException("Usuario no encontrado"));

        assertDoesNotThrow(() -> filtroAutenticacionJwt.doFilter(request, response, chain));

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
    }
}
