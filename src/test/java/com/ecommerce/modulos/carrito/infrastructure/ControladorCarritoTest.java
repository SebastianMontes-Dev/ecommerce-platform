package com.ecommerce.modulos.carrito.infrastructure;

import com.ecommerce.modulos.carrito.application.ServicioCarrito;
import com.ecommerce.modulos.carrito.domain.ArticuloCarrito;
import com.ecommerce.modulos.carrito.domain.Carrito;
import com.ecommerce.modulos.compartido.infrastructure.FiltroInquilino;
import com.ecommerce.modulos.compartido.infrastructure.InterceptorLimiteTasa;
import com.ecommerce.modulos.compartido.infrastructure.RateLimitConfig;
import com.ecommerce.modulos.compartido.infrastructure.security.FiltroAutenticacionJwt;
import com.ecommerce.modulos.compartido.infrastructure.security.FiltroRateLimit;
import com.ecommerce.modulos.identidad.application.DetallesUsuarioPersonalizado;
import com.ecommerce.modulos.ordenes.application.CasoUsoGestionarCupon;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// @WebMvcTest always scans in Filter/HandlerInterceptor/WebMvcConfigurer beans
// project-wide regardless of the controllers= narrowing (addFilters=false only skips
// *applying* Filters to mock requests, it doesn't stop their beans from being created).
// This slice doesn't exercise auth/tenant/rate-limit infra, so exclude those global
// components outright rather than mocking their growing dependency chains one by one.
@WebMvcTest(controllers = ControladorCarrito.class, excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {FiltroInquilino.class, FiltroAutenticacionJwt.class, FiltroRateLimit.class,
                InterceptorLimiteTasa.class, RateLimitConfig.class}
))
// addFilters is left at its default (true): with it false, .with(user(...))'s
// SecurityContext never gets threaded onto the request by SecurityContextHolderFilter,
// so @AuthenticationPrincipal always resolves null. The app's own Filters are already
// kept out of this slice via excludeFilters above, so enabling filter dispatch here only
// lets Spring Security's own (already-present) filters run, not the app's.
@AutoConfigureMockMvc
@ContextConfiguration(classes = com.ecommerce.bootstrap.AplicacionEcommerce.class)
class ControladorCarritoTest {

    // @EnableWebSecurity (declared on the app's SecurityConfig, not loaded in this slice)
    // is what normally registers AuthenticationPrincipalArgumentResolver — without it,
    // @AuthenticationPrincipal silently resolves to null instead of failing loudly.
    // Spring Boot's WebMvcAutoConfiguration auto-registers any HandlerMethodArgumentResolver
    // bean it finds, so declaring one directly here is the minimal, targeted fix.
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
    private ServicioCarrito servicioCarrito;

    @MockBean
    private CasoUsoGestionarCupon casoUsoGestionarCupon;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID userId;
    private DetallesUsuarioPersonalizado userDetails;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        com.ecommerce.modulos.identidad.domain.Usuario usuario = new com.ecommerce.modulos.identidad.domain.Usuario();
        usuario.setId(userId);
        usuario.setCorreo("user@test.com");
        usuario.setHashContrasena("password");
        usuario.setEnabled(true);
        usuario.setRoles(java.util.Set.of(com.ecommerce.modulos.identidad.domain.RolUsuario.CUSTOMER));
        
        userDetails = new DetallesUsuarioPersonalizado(usuario);
    }

    @Test
    void debeObtenerCarritoParaUsuarioAutenticado() throws Exception {
        Carrito carrito = new Carrito();
        carrito.setId("carrito:" + userId);

        when(servicioCarrito.getOrCreateCart(eq(userId), any())).thenReturn(carrito);

        mockMvc.perform(get("/api/v1/carrito")
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("carrito:" + userId));
    }

    @Test
    void debeAgregarArticuloParaUsuarioAutenticado() throws Exception {
        ArticuloCarrito item = new ArticuloCarrito();
        item.setIdProducto(UUID.randomUUID());
        item.setCantidad(2);
        item.setPrecioUnitario(new BigDecimal("25.00"));

        Carrito carrito = new Carrito();
        carrito.agregarArticulo(item);

        when(servicioCarrito.agregarArticulo(eq(userId), any(), any())).thenReturn(carrito);

        mockMvc.perform(post("/api/v1/carrito/articulos")
                        .with(user(userDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.articulos[0].cantidad").value(2));
    }

    @Test
    void debeAplicarCupon() throws Exception {
        String codigoCupon = "DESC10";
        Carrito carrito = new Carrito();
        ArticuloCarrito item = new ArticuloCarrito();
        item.setPrecioUnitario(new BigDecimal("50.00"));
        item.setCantidad(1);
        carrito.agregarArticulo(item);

        when(servicioCarrito.getOrCreateCart(eq(userId), any())).thenReturn(carrito);
        when(casoUsoGestionarCupon.calcularDescuento(any(), eq(codigoCupon), any())).thenReturn(new BigDecimal("10.00"));
        when(servicioCarrito.aplicarCupon(eq(userId), any(), any(), eq(codigoCupon), any())).thenReturn(carrito);

        mockMvc.perform(post("/api/v1/carrito/cupones/{codigo}", codigoCupon)
                        .with(user(userDetails))
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(servicioCarrito).aplicarCupon(eq(userId), any(), any(), eq(codigoCupon), eq(new BigDecimal("10.00")));
    }
}
