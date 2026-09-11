package com.ecommerce.modulos.pagos.infrastructure;

import com.ecommerce.modulos.compartido.infrastructure.FiltroInquilino;
import com.ecommerce.modulos.compartido.infrastructure.InterceptorLimiteTasa;
import com.ecommerce.modulos.compartido.infrastructure.RateLimitConfig;
import com.ecommerce.modulos.compartido.infrastructure.security.FiltroAutenticacionJwt;
import com.ecommerce.modulos.identidad.application.DetallesUsuarioPersonalizado;
import com.ecommerce.modulos.pagos.application.CasoUsoProcesarPago;
import com.ecommerce.modulos.pagos.domain.RepositorioPago;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// @WebMvcTest always scans in Filter/HandlerInterceptor/WebMvcConfigurer beans
// project-wide regardless of the controllers= narrowing (addFilters=false only skips
// *applying* Filters to mock requests, it doesn't stop their beans from being created).
// This slice doesn't exercise auth/tenant/rate-limit infra, so exclude those global
// components outright rather than mocking their growing dependency chains one by one.
@WebMvcTest(controllers = ControladorPago.class, excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {FiltroInquilino.class, FiltroAutenticacionJwt.class,
                InterceptorLimiteTasa.class, RateLimitConfig.class}
))
// addFilters is left at its default (true): with it false, .with(user(...))'s
// SecurityContext never gets threaded onto the request by SecurityContextHolderFilter,
// so @AuthenticationPrincipal always resolves null. The app's own Filters are already
// kept out of this slice via excludeFilters above, so enabling filter dispatch here only
// lets Spring Security's own (already-present) filters run, not the app's.
@AutoConfigureMockMvc
@ContextConfiguration(classes = com.ecommerce.bootstrap.AplicacionEcommerce.class)
class ControladorPagoTest {

    // @EnableWebSecurity (declared on the app's SecurityConfig, not loaded in this slice)
    // is what normally registers AuthenticationPrincipalArgumentResolver. Spring Boot's
    // WebMvcAutoConfiguration auto-registers any HandlerMethodArgumentResolver bean it
    // finds, so declaring one directly here is the minimal, targeted fix.
    @TestConfiguration
    static class ResolutorAutenticacionTestConfig {
        @Bean
        AuthenticationPrincipalArgumentResolver authenticationPrincipalArgumentResolver() {
            return new AuthenticationPrincipalArgumentResolver();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CasoUsoProcesarPago casoUsoProcesarPago;

    @MockBean
    private RepositorioPago repositorioPago;

    private UUID userId;
    private DetallesUsuarioPersonalizado userDetails;
    private UUID idOrden;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        com.ecommerce.modulos.identidad.domain.Usuario usuario = new com.ecommerce.modulos.identidad.domain.Usuario();
        usuario.setId(userId);
        usuario.setCorreo("pago@test.com");
        usuario.setHashContrasena("password");
        usuario.setEnabled(true);
        usuario.setRoles(java.util.Set.of(com.ecommerce.modulos.identidad.domain.RolUsuario.CUSTOMER));
        
        userDetails = new DetallesUsuarioPersonalizado(usuario);
        idOrden = UUID.randomUUID();
    }

    @Test
    void debeIniciarProcesoPago() throws Exception {
        Map<String, Object> respuestaEsperada = Map.of(
                "paymentId", UUID.randomUUID().toString(),
                "checkoutUrl", "http://checkout.url"
        );

        when(casoUsoProcesarPago.ejecutar(idOrden, userId)).thenReturn(respuestaEsperada);

        mockMvc.perform(post("/api/v1/pagos/checkout/{idOrden}", idOrden)
                        .with(user(userDetails))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkoutUrl").value("http://checkout.url"));
                
        verify(casoUsoProcesarPago).ejecutar(idOrden, userId);
    }
}
