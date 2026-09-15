package com.ecommerce.modulos.logistica.infrastructure;

import com.ecommerce.modulos.compartido.domain.ExcepcionEntidadNoEncontrada;
import com.ecommerce.modulos.compartido.infrastructure.ContextoInquilino;
import com.ecommerce.modulos.compartido.infrastructure.FiltroInquilino;
import com.ecommerce.modulos.compartido.infrastructure.InterceptorLimiteTasa;
import com.ecommerce.modulos.compartido.infrastructure.RateLimitConfig;
import com.ecommerce.modulos.compartido.infrastructure.security.FiltroAutenticacionJwt;
import com.ecommerce.modulos.logistica.application.CasoUsoLogistica;
import com.ecommerce.modulos.logistica.application.dto.RespuestaEnvio;
import com.ecommerce.modulos.logistica.domain.EstadoEnvio;
import org.junit.jupiter.api.AfterEach;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Mismo patrón que ControladorPagoTest: se excluyen los Filter/HandlerInterceptor/
// WebMvcConfigurer globales que @WebMvcTest igual detecta project-wide, ya que este
// slice no ejerce auth/tenant/rate-limit real. SecurityConfig (con @EnableMethodSecurity)
// tampoco se carga en este slice, así que @PreAuthorize("hasRole('SELLER')") no se
// aplica aquí — solo se verifica que el request llegue autenticado (requisito de
// Spring Security por defecto) y que el controller resuelva bien el tenant.
@WebMvcTest(controllers = ControladorLogistica.class, excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {FiltroInquilino.class, FiltroAutenticacionJwt.class,
                InterceptorLimiteTasa.class, RateLimitConfig.class}
))
@AutoConfigureMockMvc
@ContextConfiguration(classes = com.ecommerce.bootstrap.AplicacionEcommerce.class)
class ControladorLogisticaTest {

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
    private CasoUsoLogistica casoUsoLogistica;

    private UUID idTienda;
    private UUID idOrden;
    private String numeroGuia;

    @BeforeEach
    void setUp() {
        idTienda = UUID.randomUUID();
        idOrden = UUID.randomUUID();
        numeroGuia = "TRK-ABC1234567";
    }

    @AfterEach
    void limpiarContexto() {
        ContextoInquilino.clear();
    }

    @Test
    void debeDevolver200ConElEnvioCuandoElRastreoEsExitoso() throws Exception {
        ContextoInquilino.setIdTienda(idTienda);
        RespuestaEnvio respuesta = new RespuestaEnvio(
                idOrden, numeroGuia, "DHL_EXPRESS", "EN_TRANSITO",
                List.of(new RespuestaEnvio.EventoTracking("EN_TRANSITO", "Bogotá", "Salió del CD", LocalDateTime.now())));

        when(casoUsoLogistica.rastrearEnvio(idTienda, numeroGuia)).thenReturn(respuesta);

        mockMvc.perform(get("/api/v1/logistica/rastreo/{numeroGuia}", numeroGuia)
                        .with(user("cliente")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroGuia").value(numeroGuia))
                .andExpect(jsonPath("$.proveedor").value("DHL_EXPRESS"))
                .andExpect(jsonPath("$.estado").value("EN_TRANSITO"))
                .andExpect(jsonPath("$.historial.length()").value(1));

        verify(casoUsoLogistica).rastrearEnvio(idTienda, numeroGuia);
    }

    @Test
    void debeDevolver404CuandoElEnvioNoExisteAlRastrear() throws Exception {
        ContextoInquilino.setIdTienda(idTienda);
        when(casoUsoLogistica.rastrearEnvio(eq(idTienda), eq(numeroGuia)))
                .thenThrow(new ExcepcionEntidadNoEncontrada("Envío", numeroGuia));

        mockMvc.perform(get("/api/v1/logistica/rastreo/{numeroGuia}", numeroGuia)
                        .with(user("cliente")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    void debeDevolver200ConElEstadoActualizadoCuandoLaOperacionEsExitosa() throws Exception {
        ContextoInquilino.setIdTiendaPropia(idTienda);
        RespuestaEnvio respuesta = new RespuestaEnvio(
                idOrden, numeroGuia, "DHL_EXPRESS", "ENTREGADO",
                List.of(new RespuestaEnvio.EventoTracking("ENTREGADO", "Casa", "Entregado al cliente", LocalDateTime.now())));

        when(casoUsoLogistica.actualizarEstado(idTienda, numeroGuia, EstadoEnvio.ENTREGADO, "Casa", "Entregado al cliente"))
                .thenReturn(respuesta);

        mockMvc.perform(post("/api/v1/logistica/admin/rastreo/{numeroGuia}/estado", numeroGuia)
                        .param("estado", "ENTREGADO")
                        .param("ubicacion", "Casa")
                        .param("descripcion", "Entregado al cliente")
                        .with(user("vendedor").roles("SELLER"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ENTREGADO"));

        verify(casoUsoLogistica).actualizarEstado(idTienda, numeroGuia, EstadoEnvio.ENTREGADO, "Casa", "Entregado al cliente");
    }

    @Test
    void debeDevolver404CuandoElEnvioNoExisteAlActualizarEstado() throws Exception {
        ContextoInquilino.setIdTiendaPropia(idTienda);
        when(casoUsoLogistica.actualizarEstado(eq(idTienda), eq(numeroGuia), eq(EstadoEnvio.EN_TRANSITO), eq("Bogotá"), eq("Salió del CD")))
                .thenThrow(new ExcepcionEntidadNoEncontrada("Envío", numeroGuia));

        mockMvc.perform(post("/api/v1/logistica/admin/rastreo/{numeroGuia}/estado", numeroGuia)
                        .param("estado", "EN_TRANSITO")
                        .param("ubicacion", "Bogotá")
                        .param("descripcion", "Salió del CD")
                        .with(user("vendedor").roles("SELLER"))
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void debeDevolver401CuandoElUsuarioNoTieneTiendaPropiaAlActualizarEstado() throws Exception {
        // No se llama a ContextoInquilino.setIdTiendaPropia(...): simula un usuario
        // autenticado sin tienda propia resuelta server-side.
        mockMvc.perform(post("/api/v1/logistica/admin/rastreo/{numeroGuia}/estado", numeroGuia)
                        .param("estado", "EN_TRANSITO")
                        .param("ubicacion", "Bogotá")
                        .param("descripcion", "Salió del CD")
                        .with(user("vendedor").roles("SELLER"))
                        .with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Unauthorized"));
    }

    @Test
    void debeDevolver500CuandoElEstadoEnviadoNoEsUnValorValidoDelEnum() throws Exception {
        // ManejadorExcepcionGlobal no tiene un @ExceptionHandler específico para
        // MethodArgumentTypeMismatchException (el error de binding de un @RequestParam
        // enum inválido) — cae en el handler genérico de Exception, que responde 500.
        // Se documenta el comportamiento real tal cual existe hoy, no el ideal.
        ContextoInquilino.setIdTiendaPropia(idTienda);

        mockMvc.perform(post("/api/v1/logistica/admin/rastreo/{numeroGuia}/estado", numeroGuia)
                        .param("estado", "ESTADO_INEXISTENTE")
                        .param("ubicacion", "Bogotá")
                        .param("descripcion", "Salió del CD")
                        .with(user("vendedor").roles("SELLER"))
                        .with(csrf()))
                .andExpect(status().isInternalServerError());
    }
}
