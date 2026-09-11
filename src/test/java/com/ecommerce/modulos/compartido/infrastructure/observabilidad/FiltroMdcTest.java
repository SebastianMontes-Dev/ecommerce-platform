package com.ecommerce.modulos.compartido.infrastructure.observabilidad;

import com.ecommerce.modulos.compartido.infrastructure.ContextoInquilino;
import com.ecommerce.modulos.identidad.application.DetallesUsuarioPersonalizado;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FiltroMdcTest {

    private final FiltroMdc filtro = new FiltroMdc();

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
        ContextoInquilino.clear();
        MDC.clear();
    }

    @Test
    void copiaTenantYUsuarioAlMdcDuranteLaCadenaYLosLimpiaAlSalir() throws Exception {
        UUID idTienda = UUID.randomUUID();
        UUID idUsuario = UUID.randomUUID();
        ContextoInquilino.setIdTienda(idTienda);
        autenticarUsuario(idUsuario);

        Map<String, String> mdcDuranteLaCadena = new HashMap<>();
        FilterChain cadena = (req, res) -> {
            mdcDuranteLaCadena.put("tenantId", MDC.get("tenantId"));
            mdcDuranteLaCadena.put("userId", MDC.get("userId"));
        };

        filtro.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), cadena);

        assertEquals(idTienda.toString(), mdcDuranteLaCadena.get("tenantId"));
        assertEquals(idUsuario.toString(), mdcDuranteLaCadena.get("userId"));
        assertNull(MDC.get("tenantId"), "el MDC debe quedar limpio tras la request");
        assertNull(MDC.get("userId"));
    }

    @Test
    void sinInquilinoNiUsuarioNoPoneClavesEnElMdc() throws Exception {
        boolean[] cadenaEjecutada = {false};
        FilterChain cadena = (req, res) -> {
            cadenaEjecutada[0] = true;
            assertNull(MDC.get("tenantId"));
            assertNull(MDC.get("userId"));
        };

        filtro.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), cadena);

        assertTrue(cadenaEjecutada[0]);
    }

    @Test
    void limpiaElMdcAunqueLaCadenaLance() {
        ContextoInquilino.setIdTienda(UUID.randomUUID());
        FilterChain cadena = (req, res) -> { throw new RuntimeException("boom"); };

        assertThrows(RuntimeException.class,
                () -> filtro.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), cadena));

        assertNull(MDC.get("tenantId"));
    }

    private static void autenticarUsuario(UUID idUsuario) {
        DetallesUsuarioPersonalizado detalles = mock(DetallesUsuarioPersonalizado.class);
        when(detalles.getUserId()).thenReturn(idUsuario);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(detalles, null, List.of()));
    }
}
