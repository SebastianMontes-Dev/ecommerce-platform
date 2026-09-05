package com.ecommerce.modulos.compartido.infrastructure.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FiltroRateLimitTest {

    private final FiltroRateLimit filtro = new FiltroRateLimit();

    @Test
    void dejaPasarPeticionesAEndpointsNoProtegidosSinLimitarlas() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/productos");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        for (int i = 0; i < 50; i++) {
            filtro.doFilter(request, response, chain);
        }

        verify(chain, times(50)).doFilter(request, response);
        assertNotEquals(HttpStatus.TOO_MANY_REQUESTS.value(), response.getStatus());
    }

    @Test
    void dejaPasarPeticionesDentroDelLimiteEnUnEndpointProtegido() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/ordenes/checkout");
        request.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        for (int i = 0; i < 10; i++) {
            filtro.doFilter(request, response, chain);
        }

        verify(chain, times(10)).doFilter(request, response);
    }

    @Test
    void bloqueaConDemasiadasPeticionesCuandoSeExcedeElLimiteEnUnEndpointProtegido() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/ordenes/checkout");
        request.setRemoteAddr("10.0.0.2");
        FilterChain chain = mock(FilterChain.class);

        for (int i = 0; i < 10; i++) {
            filtro.doFilter(request, new MockHttpServletResponse(), chain);
        }

        MockHttpServletResponse respuestaExcedida = new MockHttpServletResponse();
        filtro.doFilter(request, respuestaExcedida, chain);

        assertEquals(HttpStatus.TOO_MANY_REQUESTS.value(), respuestaExcedida.getStatus());
        verify(chain, times(10)).doFilter(any(), any());
    }

    @Test
    void aplicaElLimitePorIpDeFormaIndependiente() throws Exception {
        FilterChain chain = mock(FilterChain.class);

        MockHttpServletRequest requestIpUno = new MockHttpServletRequest();
        requestIpUno.setRequestURI("/api/v1/chatbot/chat");
        requestIpUno.setRemoteAddr("10.0.0.3");
        for (int i = 0; i < 10; i++) {
            filtro.doFilter(requestIpUno, new MockHttpServletResponse(), chain);
        }

        MockHttpServletRequest requestIpDos = new MockHttpServletRequest();
        requestIpDos.setRequestURI("/api/v1/chatbot/chat");
        requestIpDos.setRemoteAddr("10.0.0.4");
        MockHttpServletResponse respuestaIpDos = new MockHttpServletResponse();
        filtro.doFilter(requestIpDos, respuestaIpDos, chain);

        assertNotEquals(HttpStatus.TOO_MANY_REQUESTS.value(), respuestaIpDos.getStatus());
    }

    @Test
    void usaLaPrimeraIpDeXForwardedForCuandoEstaPresente() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/ordenes/checkout");
        request.addHeader("X-Forwarded-For", "203.0.113.10, 10.0.0.1");
        FilterChain chain = mock(FilterChain.class);

        for (int i = 0; i < 10; i++) {
            filtro.doFilter(request, new MockHttpServletResponse(), chain);
        }

        MockHttpServletResponse respuestaExcedida = new MockHttpServletResponse();
        filtro.doFilter(request, respuestaExcedida, chain);

        assertEquals(HttpStatus.TOO_MANY_REQUESTS.value(), respuestaExcedida.getStatus());
    }
}
