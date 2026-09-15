package com.ecommerce.modulos.ordenes.infrastructure;

import com.ecommerce.modulos.compartido.domain.ExcepcionEntidadNoEncontrada;
import com.ecommerce.modulos.compartido.domain.ExcepcionRecursoDuplicado;
import com.ecommerce.modulos.compartido.infrastructure.ContextoInquilino;
import com.ecommerce.modulos.compartido.infrastructure.FiltroInquilino;
import com.ecommerce.modulos.compartido.infrastructure.InterceptorLimiteTasa;
import com.ecommerce.modulos.compartido.infrastructure.RateLimitConfig;
import com.ecommerce.modulos.compartido.infrastructure.RespuestaPaginada;
import com.ecommerce.modulos.compartido.infrastructure.security.FiltroAutenticacionJwt;
import com.ecommerce.modulos.identidad.application.DetallesUsuarioPersonalizado;
import com.ecommerce.modulos.identidad.domain.RolUsuario;
import com.ecommerce.modulos.identidad.domain.Usuario;
import com.ecommerce.modulos.ordenes.application.CasoUsoGestionarCupon;
import com.ecommerce.modulos.ordenes.application.dto.RespuestaCupon;
import com.ecommerce.modulos.ordenes.application.dto.SolicitudCrearCupon;
import com.ecommerce.modulos.ordenes.domain.TipoDescuento;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
// NOTE: @EnableMethodSecurity vive en SecurityConfig, un @Configuration que esta slice no
// carga, así que el @PreAuthorize("hasRole('SELLER')") de la clase queda inerte aquí y no
// se re-testea en esta clase. Lo que sí se ejercita (y se prueba) es
// ContextoInquilino.getIdTiendaPropia(), que lanza ExcepcionNoAutorizado (401) por su cuenta
// sin depender de Spring Security.
@WebMvcTest(controllers = ControladorCupon.class, excludeFilters = @ComponentScan.Filter(
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
class ControladorCuponTest {

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

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CasoUsoGestionarCupon casoUsoGestionarCupon;

    private DetallesUsuarioPersonalizado userDetails;
    private UUID idTienda;

    @BeforeEach
    void setUp() {
        UUID userId = UUID.randomUUID();
        Usuario usuario = new Usuario();
        usuario.setId(userId);
        usuario.setCorreo("vendedor@test.com");
        usuario.setHashContrasena("password");
        usuario.setEnabled(true);
        usuario.setRoles(Set.of(RolUsuario.SELLER));

        userDetails = new DetallesUsuarioPersonalizado(usuario);

        idTienda = UUID.randomUUID();
        ContextoInquilino.setIdTiendaPropia(idTienda);
    }

    @AfterEach
    void limpiarContexto() {
        ContextoInquilino.clear();
    }

    private SolicitudCrearCupon solicitudValida() {
        return SolicitudCrearCupon.builder()
                .codigo("PROMO10")
                .tipo(TipoDescuento.PORCENTAJE)
                .valor(new BigDecimal("10"))
                .limiteUsos(100)
                .build();
    }

    private RespuestaCupon respuestaCupon(UUID id) {
        return RespuestaCupon.builder()
                .id(id)
                .codigo("PROMO10")
                .tipo(TipoDescuento.PORCENTAJE)
                .valor(new BigDecimal("10"))
                .limiteUsos(100)
                .usosActuales(0)
                .activo(true)
                .creadoEn(LocalDateTime.now())
                .build();
    }

    @Test
    void debeCrearUnCuponYResponder201() throws Exception {
        UUID idCupon = UUID.randomUUID();
        when(casoUsoGestionarCupon.crearCupon(eq(idTienda), any(SolicitudCrearCupon.class)))
                .thenReturn(respuestaCupon(idCupon));

        mockMvc.perform(post("/api/v1/cupones")
                        .with(user(userDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(solicitudValida())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(idCupon.toString()))
                .andExpect(jsonPath("$.codigo").value("PROMO10"));

        verify(casoUsoGestionarCupon).crearCupon(eq(idTienda), any(SolicitudCrearCupon.class));
    }

    @Test
    void debeResponder400SiElCodigoEstaEnBlanco() throws Exception {
        SolicitudCrearCupon solicitud = SolicitudCrearCupon.builder()
                .codigo(" ")
                .tipo(TipoDescuento.PORCENTAJE)
                .valor(new BigDecimal("10"))
                .build();

        mockMvc.perform(post("/api/v1/cupones")
                        .with(user(userDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(solicitud)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void debeResponder400SiElDescuentoPorcentualSuperaCienPorCiento() throws Exception {
        SolicitudCrearCupon solicitud = SolicitudCrearCupon.builder()
                .codigo("PROMO500")
                .tipo(TipoDescuento.PORCENTAJE)
                .valor(new BigDecimal("500"))
                .build();

        mockMvc.perform(post("/api/v1/cupones")
                        .with(user(userDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(solicitud)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void debeResponder400SiElValorDelDescuentoNoEsPositivo() throws Exception {
        SolicitudCrearCupon solicitud = SolicitudCrearCupon.builder()
                .codigo("PROMO0")
                .tipo(TipoDescuento.MONTO_FIJO)
                .valor(new BigDecimal("-5"))
                .build();

        mockMvc.perform(post("/api/v1/cupones")
                        .with(user(userDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(solicitud)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void debeResponder409SiElCodigoDeCuponYaExiste() throws Exception {
        when(casoUsoGestionarCupon.crearCupon(eq(idTienda), any(SolicitudCrearCupon.class)))
                .thenThrow(new ExcepcionRecursoDuplicado("Cupón", "código", "PROMO10"));

        mockMvc.perform(post("/api/v1/cupones")
                        .with(user(userDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(solicitudValida())))
                .andExpect(status().isConflict());
    }

    @Test
    void debeResponder401AlCrearCuponSinTiendaPropia() throws Exception {
        ContextoInquilino.clear();

        mockMvc.perform(post("/api/v1/cupones")
                        .with(user(userDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(solicitudValida())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void debeListarLosCuponesDeLaTiendaPropia() throws Exception {
        RespuestaCupon cupon = respuestaCupon(UUID.randomUUID());
        RespuestaPaginada<RespuestaCupon> pagina = RespuestaPaginada.from(
                new PageImpl<>(List.of(cupon), PageRequest.of(0, 20), 1));

        when(casoUsoGestionarCupon.listarCupones(eq(idTienda), any())).thenReturn(pagina);

        mockMvc.perform(get("/api/v1/cupones")
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].codigo").value("PROMO10"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void debeAlternarElEstadoDeUnCuponYResponder204() throws Exception {
        UUID idCupon = UUID.randomUUID();

        mockMvc.perform(patch("/api/v1/cupones/{idCupon}/estado", idCupon)
                        .with(user(userDetails))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(casoUsoGestionarCupon).alternarEstadoCupon(idTienda, idCupon);
    }

    @Test
    void debeResponder404AlAlternarEstadoDeUnCuponInexistente() throws Exception {
        UUID idCupon = UUID.randomUUID();
        org.mockito.Mockito.doThrow(new ExcepcionEntidadNoEncontrada("Cupón", idCupon))
                .when(casoUsoGestionarCupon).alternarEstadoCupon(idTienda, idCupon);

        mockMvc.perform(patch("/api/v1/cupones/{idCupon}/estado", idCupon)
                        .with(user(userDetails))
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }
}
