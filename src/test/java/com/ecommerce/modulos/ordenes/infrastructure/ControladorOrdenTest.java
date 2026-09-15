package com.ecommerce.modulos.ordenes.infrastructure;

import com.ecommerce.modulos.compartido.domain.Direccion;
import com.ecommerce.modulos.compartido.domain.ExcepcionEntidadNoEncontrada;
import com.ecommerce.modulos.compartido.domain.ExcepcionOperacionInvalida;
import com.ecommerce.modulos.compartido.infrastructure.ContextoInquilino;
import com.ecommerce.modulos.compartido.infrastructure.FiltroInquilino;
import com.ecommerce.modulos.compartido.infrastructure.InterceptorLimiteTasa;
import com.ecommerce.modulos.compartido.infrastructure.RateLimitConfig;
import com.ecommerce.modulos.compartido.infrastructure.RespuestaPaginada;
import com.ecommerce.modulos.compartido.infrastructure.security.FiltroAutenticacionJwt;
import com.ecommerce.modulos.identidad.application.DetallesUsuarioPersonalizado;
import com.ecommerce.modulos.identidad.domain.RolUsuario;
import com.ecommerce.modulos.identidad.domain.Usuario;
import com.ecommerce.modulos.ordenes.application.CasoUsoOrden;
import com.ecommerce.modulos.ordenes.application.RespuestaOrden;
import com.ecommerce.modulos.ordenes.application.ServicioReporteOrdenes;
import com.ecommerce.modulos.ordenes.application.dto.SolicitudCheckout;
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
// NOTE: this also means @EnableMethodSecurity (declared on SecurityConfig, a @Configuration
// bean this slice never loads) is not woven in, so @PreAuthorize("hasRole('SELLER')") is
// inert here — it is not re-tested in this class. What IS enforced (and tested) at this
// layer is ContextoInquilino.getIdTiendaPropia(), which throws ExcepcionNoAutorizado (401)
// on its own regardless of Spring Security.
@WebMvcTest(controllers = ControladorOrden.class, excludeFilters = @ComponentScan.Filter(
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
class ControladorOrdenTest {

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
    private CasoUsoOrden casoUsoOrden;

    @MockBean
    private ServicioReporteOrdenes servicioReporteOrdenes;

    private UUID userId;
    private DetallesUsuarioPersonalizado userDetails;
    private UUID idTienda;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        Usuario usuario = new Usuario();
        usuario.setId(userId);
        usuario.setCorreo("cliente@test.com");
        usuario.setHashContrasena("password");
        usuario.setEnabled(true);
        usuario.setRoles(Set.of(RolUsuario.CUSTOMER));

        userDetails = new DetallesUsuarioPersonalizado(usuario);

        idTienda = UUID.randomUUID();
        ContextoInquilino.setIdTienda(idTienda);
        ContextoInquilino.setIdTiendaPropia(idTienda);
    }

    @AfterEach
    void limpiarContexto() {
        ContextoInquilino.clear();
    }

    private SolicitudCheckout solicitudCheckoutValida() {
        SolicitudCheckout solicitud = new SolicitudCheckout();
        solicitud.setDireccionEnvio(Direccion.of("Calle 1", "Lima", "Lima", "15001", "PE"));
        solicitud.setDireccionFacturacion(Direccion.of("Calle 1", "Lima", "Lima", "15001", "PE"));
        solicitud.setNotas("Entregar en la tarde");
        return solicitud;
    }

    private RespuestaOrden respuestaOrden(UUID id) {
        return RespuestaOrden.builder()
                .id(id)
                .numeroOrden("ABCD1234")
                .idCliente(userId)
                .correoCliente("cliente@test.com")
                .nombreCliente("Cliente Test")
                .subtotal(new BigDecimal("50.00"))
                .montoImpuesto(new BigDecimal("5.00"))
                .montoEnvio(new BigDecimal("10.00"))
                .total(new BigDecimal("65.00"))
                .moneda("USD")
                .estado("CREATED")
                .creadoEn(LocalDateTime.now())
                .build();
    }

    @Test
    void debeCrearOrdenDesdeElCarritoYResponder201() throws Exception {
        UUID idOrden = UUID.randomUUID();
        RespuestaOrden respuesta = respuestaOrden(idOrden);

        when(casoUsoOrden.createOrderFromCart(eq(userId), eq(idTienda), any(SolicitudCheckout.class)))
                .thenReturn(respuesta);

        mockMvc.perform(post("/api/v1/ordenes/checkout")
                        .with(user(userDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(solicitudCheckoutValida())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(idOrden.toString()))
                .andExpect(jsonPath("$.estado").value("CREATED"));

        verify(casoUsoOrden).createOrderFromCart(eq(userId), eq(idTienda), any(SolicitudCheckout.class));
    }

    @Test
    void debeResponder400SiFaltaDireccionEnvioEnCheckout() throws Exception {
        SolicitudCheckout solicitud = new SolicitudCheckout();
        solicitud.setDireccionFacturacion(Direccion.of("Calle 1", "Lima", "Lima", "15001", "PE"));
        // direccionEnvio queda null a propósito: viola @NotNull

        mockMvc.perform(post("/api/v1/ordenes/checkout")
                        .with(user(userDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(solicitud)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void debeResponder409SiElCarritoEstaVacio() throws Exception {
        when(casoUsoOrden.createOrderFromCart(eq(userId), eq(idTienda), any(SolicitudCheckout.class)))
                .thenThrow(new ExcepcionOperacionInvalida("Cannot checkout with an empty cart"));

        mockMvc.perform(post("/api/v1/ordenes/checkout")
                        .with(user(userDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(solicitudCheckoutValida())))
                .andExpect(status().isConflict());
    }

    @Test
    void debeListarOrdenesDeLaTiendaPropia() throws Exception {
        RespuestaOrden respuesta = respuestaOrden(UUID.randomUUID());
        RespuestaPaginada<RespuestaOrden> pagina = RespuestaPaginada.from(
                new PageImpl<>(List.of(respuesta), PageRequest.of(0, 20), 1));

        when(casoUsoOrden.listOrdersByTenant(eq(idTienda), any())).thenReturn(pagina);

        mockMvc.perform(get("/api/v1/ordenes")
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(respuesta.getId().toString()))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void debeResponder401AlListarOrdenesSinTiendaPropia() throws Exception {
        ContextoInquilino.clear();
        ContextoInquilino.setIdTienda(idTienda);
        // OWNED_TENANT queda sin setear: simula un usuario autenticado que no es dueño de tienda.

        mockMvc.perform(get("/api/v1/ordenes")
                        .with(user(userDetails)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void debeObtenerOrdenPorId() throws Exception {
        UUID idOrden = UUID.randomUUID();
        RespuestaOrden respuesta = respuestaOrden(idOrden);

        when(casoUsoOrden.getOrder(idOrden, idTienda, userId)).thenReturn(respuesta);

        mockMvc.perform(get("/api/v1/ordenes/{id}", idOrden)
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroOrden").value("ABCD1234"));
    }

    @Test
    void debeResponder404SiLaOrdenNoExiste() throws Exception {
        UUID idOrden = UUID.randomUUID();

        when(casoUsoOrden.getOrder(idOrden, idTienda, userId))
                .thenThrow(new ExcepcionEntidadNoEncontrada("Orden", idOrden));

        mockMvc.perform(get("/api/v1/ordenes/{id}", idOrden)
                        .with(user(userDetails)))
                .andExpect(status().isNotFound());
    }

    @Test
    void debeCancelarUnaOrdenConMotivo() throws Exception {
        UUID idOrden = UUID.randomUUID();
        RespuestaOrden respuesta = respuestaOrden(idOrden);
        respuesta.setEstado("CANCELLED");

        when(casoUsoOrden.cancelOrder(idOrden, idTienda, userId, "Cambié de opinión")).thenReturn(respuesta);

        mockMvc.perform(post("/api/v1/ordenes/{id}/cancel", idOrden)
                        .param("reason", "Cambié de opinión")
                        .with(user(userDetails))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELLED"));

        verify(casoUsoOrden).cancelOrder(idOrden, idTienda, userId, "Cambié de opinión");
    }

    @Test
    void debeCancelarUnaOrdenSinMotivoUsandoElValorPorDefecto() throws Exception {
        UUID idOrden = UUID.randomUUID();
        RespuestaOrden respuesta = respuestaOrden(idOrden);
        respuesta.setEstado("CANCELLED");

        when(casoUsoOrden.cancelOrder(idOrden, idTienda, userId, "")).thenReturn(respuesta);

        mockMvc.perform(post("/api/v1/ordenes/{id}/cancel", idOrden)
                        .with(user(userDetails))
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(casoUsoOrden).cancelOrder(idOrden, idTienda, userId, "");
    }

    @Test
    void debeResponder404AlCancelarUnaOrdenInexistente() throws Exception {
        UUID idOrden = UUID.randomUUID();

        when(casoUsoOrden.cancelOrder(eq(idOrden), eq(idTienda), eq(userId), any()))
                .thenThrow(new ExcepcionEntidadNoEncontrada("Orden", idOrden));

        mockMvc.perform(post("/api/v1/ordenes/{id}/cancel", idOrden)
                        .with(user(userDetails))
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void debeGenerarElReporteExcelDeLaTiendaPropia() throws Exception {
        byte[] contenido = new byte[]{1, 2, 3, 4};
        when(servicioReporteOrdenes.generarReporteExcel(idTienda)).thenReturn(contenido);

        mockMvc.perform(get("/api/v1/ordenes/reporte/excel")
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"reporte_ordenes.xlsx\""))
                .andExpect(content().bytes(contenido));
    }

    @Test
    void debeResponder401AlGenerarReporteSinTiendaPropia() throws Exception {
        ContextoInquilino.clear();

        mockMvc.perform(get("/api/v1/ordenes/reporte/excel")
                        .with(user(userDetails)))
                .andExpect(status().isUnauthorized());
    }
}
