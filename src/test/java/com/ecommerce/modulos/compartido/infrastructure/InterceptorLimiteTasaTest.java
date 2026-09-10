package com.ecommerce.modulos.compartido.infrastructure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InterceptorLimiteTasaTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private InterceptorLimiteTasa interceptor;

    private void configurarInterceptor() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        interceptor = new InterceptorLimiteTasa(redisTemplate);
    }

    @Test
    void permiteLaPeticionCuandoElContadorEstaDentroDelLimite() throws Exception {
        configurarInterceptor();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/productos");
        request.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(valueOperations.increment("rate:api:10.0.0.1")).thenReturn(1L);

        boolean continuar = interceptor.preHandle(request, response, new Object());

        assertTrue(continuar);
        verify(redisTemplate).expire("rate:api:10.0.0.1", Duration.ofMinutes(1));
        assertEquals("60", response.getHeader("X-RateLimit-Limit"));
    }

    @Test
    void bloqueaConDemasiadasPeticionesCuandoSeSuperaElLimiteEstandar() throws Exception {
        configurarInterceptor();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/productos");
        request.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(valueOperations.increment("rate:api:10.0.0.1")).thenReturn(61L);

        boolean continuar = interceptor.preHandle(request, response, new Object());

        assertFalse(continuar);
        assertEquals(HttpStatus.TOO_MANY_REQUESTS.value(), response.getStatus());
        assertEquals("application/problem+json", response.getContentType());
    }

    @Test
    void usaElLimiteMasEstrictoParaElEndpointDeLogin() throws Exception {
        configurarInterceptor();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/auth/login");
        request.setRemoteAddr("10.0.0.2");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(valueOperations.increment("rate:auth:10.0.0.2")).thenReturn(11L);

        boolean continuar = interceptor.preHandle(request, response, new Object());

        assertFalse(continuar);
        assertEquals(HttpStatus.TOO_MANY_REQUESTS.value(), response.getStatus());
    }

    @Test
    void permiteHastaElLimiteExactoDePeticionesDeAutenticacion() throws Exception {
        configurarInterceptor();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/auth/registro");
        request.setRemoteAddr("10.0.0.3");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(valueOperations.increment("rate:auth:10.0.0.3")).thenReturn(10L);

        boolean continuar = interceptor.preHandle(request, response, new Object());

        assertTrue(continuar);
        assertEquals("0", response.getHeader("X-RateLimit-Remaining"));
    }

    @Test
    void aplicaElLimiteEstrictoAlEndpointRealDeRegistro() throws Exception {
        configurarInterceptor();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/auth/registro");
        request.setRemoteAddr("10.0.0.9");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // 11 supera el cubo estricto de 10/min; con el bug (/register) habría pasado (cubo de 60).
        when(valueOperations.increment("rate:auth:10.0.0.9")).thenReturn(11L);

        boolean continuar = interceptor.preHandle(request, response, new Object());

        assertFalse(continuar);
        assertEquals(HttpStatus.TOO_MANY_REQUESTS.value(), response.getStatus());
    }

    @Test
    void noReseteaElTtlSiElContadorYaExistiaPreviamente() throws Exception {
        configurarInterceptor();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/productos");
        request.setRemoteAddr("10.0.0.4");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(valueOperations.increment("rate:api:10.0.0.4")).thenReturn(5L);

        interceptor.preHandle(request, response, new Object());

        verify(redisTemplate, never()).expire(any(), any());
    }

    @Test
    void usaLaPrimeraIpDeXForwardedForCuandoEstaPresente() throws Exception {
        configurarInterceptor();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/productos");
        request.addHeader("X-Forwarded-For", "203.0.113.5, 10.0.0.1");
        request.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(valueOperations.increment("rate:api:203.0.113.5")).thenReturn(1L);

        interceptor.preHandle(request, response, new Object());

        verify(valueOperations).increment("rate:api:203.0.113.5");
    }
}
