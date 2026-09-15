package com.ecommerce.modulos.resenas.infrastructure;

import com.ecommerce.modulos.compartido.domain.ExcepcionOperacionInvalida;
import com.ecommerce.modulos.compartido.infrastructure.FiltroInquilino;
import com.ecommerce.modulos.compartido.infrastructure.InterceptorLimiteTasa;
import com.ecommerce.modulos.compartido.infrastructure.RateLimitConfig;
import com.ecommerce.modulos.compartido.infrastructure.RespuestaPaginada;
import com.ecommerce.modulos.compartido.infrastructure.security.FiltroAutenticacionJwt;
import com.ecommerce.modulos.identidad.application.DetallesUsuarioPersonalizado;
import com.ecommerce.modulos.identidad.domain.RolUsuario;
import com.ecommerce.modulos.identidad.domain.Usuario;
import com.ecommerce.modulos.resenas.application.CasoUsoConsultarResenas;
import com.ecommerce.modulos.resenas.application.CasoUsoCrearResena;
import com.ecommerce.modulos.resenas.application.dto.RespuestaResena;
import com.ecommerce.modulos.resenas.application.dto.SolicitudCrearResena;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// @WebMvcTest always scans in Filter/HandlerInterceptor/WebMvcConfigurer beans
// project-wide regardless of the controllers= narrowing (addFilters=false only skips
// *applying* Filters to mock requests, it doesn't stop their beans from being created).
// This slice doesn't exercise auth/tenant/rate-limit infra, so exclude those global
// components outright rather than mocking their growing dependency chains one by one.
@WebMvcTest(controllers = ControladorResena.class, excludeFilters = @ComponentScan.Filter(
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
class ControladorResenaTest {

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
    private CasoUsoConsultarResenas casoUsoConsultarResenas;

    @MockBean
    private CasoUsoCrearResena casoUsoCrearResena;

    private UUID idProducto;
    private UUID userId;
    private DetallesUsuarioPersonalizado userDetails;
    private SolicitudCrearResena solicitudValida;

    @BeforeEach
    void setUp() {
        idProducto = UUID.randomUUID();
        userId = UUID.randomUUID();

        Usuario usuario = new Usuario();
        usuario.setId(userId);
        usuario.setCorreo("resena@test.com");
        usuario.setHashContrasena("password");
        usuario.setEnabled(true);
        usuario.setRoles(Set.of(RolUsuario.CUSTOMER));
        userDetails = new DetallesUsuarioPersonalizado(usuario);

        solicitudValida = new SolicitudCrearResena();
        solicitudValida.setIdOrden(UUID.randomUUID());
        solicitudValida.setCalificacion(5);
        solicitudValida.setTitulo("Excelente producto");
        solicitudValida.setComentario("Cumplió mis expectativas");
    }

    private RespuestaResena respuestaDe(UUID idProducto, int calificacion, String titulo) {
        return new RespuestaResena(
                UUID.randomUUID(), idProducto, BigDecimal.valueOf(calificacion), titulo,
                "comentario", LocalDateTime.now());
    }

    @Test
    void debeListarResenasActivasConParametrosDePaginacionPorDefecto() throws Exception {
        RespuestaResena resena = respuestaDe(idProducto, 5, "Muy bueno");
        RespuestaPaginada<RespuestaResena> pagina = RespuestaPaginada.from(
                new PageImpl<>(List.of(resena), PageRequest.of(0, 10), 1));

        when(casoUsoConsultarResenas.listarActivasDeProducto(eq(idProducto), eq(PageRequest.of(0, 10))))
                .thenReturn(pagina);

        mockMvc.perform(get("/api/v1/productos/{idProducto}/resenas", idProducto)
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].titulo").value("Muy bueno"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(casoUsoConsultarResenas).listarActivasDeProducto(idProducto, PageRequest.of(0, 10));
    }

    @Test
    void debeListarResenasRespetandoLosParametrosDePaginacionRecibidos() throws Exception {
        RespuestaPaginada<RespuestaResena> pagina = RespuestaPaginada.from(
                new PageImpl<>(List.of(), PageRequest.of(2, 5), 0));

        when(casoUsoConsultarResenas.listarActivasDeProducto(eq(idProducto), eq(PageRequest.of(2, 5))))
                .thenReturn(pagina);

        mockMvc.perform(get("/api/v1/productos/{idProducto}/resenas", idProducto)
                        .with(user(userDetails))
                        .param("page", "2")
                        .param("size", "5"))
                .andExpect(status().isOk());

        verify(casoUsoConsultarResenas).listarActivasDeProducto(idProducto, PageRequest.of(2, 5));
    }

    @Test
    void debeRetornarListaVaciaCuandoElProductoNoTieneResenasActivas() throws Exception {
        RespuestaPaginada<RespuestaResena> paginaVacia = RespuestaPaginada.from(
                new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        when(casoUsoConsultarResenas.listarActivasDeProducto(eq(idProducto), eq(PageRequest.of(0, 10))))
                .thenReturn(paginaVacia);

        mockMvc.perform(get("/api/v1/productos/{idProducto}/resenas", idProducto)
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void debeCrearResenaExitosamenteParaUsuarioAutenticado() throws Exception {
        RespuestaResena respuesta = respuestaDe(idProducto, 5, "Excelente producto");
        when(casoUsoCrearResena.ejecutar(eq(idProducto), eq(userId), any(SolicitudCrearResena.class)))
                .thenReturn(respuesta);

        mockMvc.perform(post("/api/v1/productos/{idProducto}/resenas", idProducto)
                        .with(user(userDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(solicitudValida)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titulo").value("Excelente producto"))
                .andExpect(jsonPath("$.calificacion").value(5));

        verify(casoUsoCrearResena).ejecutar(eq(idProducto), eq(userId), any(SolicitudCrearResena.class));
    }

    @Test
    void debeRetornarBadRequestCuandoLaCalificacionEsMenorAlMinimo() throws Exception {
        solicitudValida.setCalificacion(0);

        mockMvc.perform(post("/api/v1/productos/{idProducto}/resenas", idProducto)
                        .with(user(userDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(solicitudValida)))
                .andExpect(status().isBadRequest());

        verify(casoUsoCrearResena, never()).ejecutar(any(), any(), any());
    }

    @Test
    void debeRetornarBadRequestCuandoLaCalificacionSuperaElMaximo() throws Exception {
        solicitudValida.setCalificacion(6);

        mockMvc.perform(post("/api/v1/productos/{idProducto}/resenas", idProducto)
                        .with(user(userDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(solicitudValida)))
                .andExpect(status().isBadRequest());

        verify(casoUsoCrearResena, never()).ejecutar(any(), any(), any());
    }

    @Test
    void debeRetornarBadRequestCuandoElTituloEstaVacio() throws Exception {
        solicitudValida.setTitulo("");

        mockMvc.perform(post("/api/v1/productos/{idProducto}/resenas", idProducto)
                        .with(user(userDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(solicitudValida)))
                .andExpect(status().isBadRequest());

        verify(casoUsoCrearResena, never()).ejecutar(any(), any(), any());
    }

    @Test
    void debeRetornarBadRequestCuandoFaltaElIdDeLaOrden() throws Exception {
        solicitudValida.setIdOrden(null);

        mockMvc.perform(post("/api/v1/productos/{idProducto}/resenas", idProducto)
                        .with(user(userDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(solicitudValida)))
                .andExpect(status().isBadRequest());

        verify(casoUsoCrearResena, never()).ejecutar(any(), any(), any());
    }

    @Test
    void debeRetornarConflictCuandoLaCompraNoEsElegibleParaResena() throws Exception {
        when(casoUsoCrearResena.ejecutar(eq(idProducto), eq(userId), any(SolicitudCrearResena.class)))
                .thenThrow(new ExcepcionOperacionInvalida(
                        "Solo se pueden reseñar productos de órdenes entregadas."));

        mockMvc.perform(post("/api/v1/productos/{idProducto}/resenas", idProducto)
                        .with(user(userDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(solicitudValida)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Solo se pueden reseñar productos de órdenes entregadas."));
    }

    @Test
    void debeRetornarConflictCuandoNoHayUsuarioAutenticado() throws Exception {
        // Un principal que no es DetallesUsuarioPersonalizado hace que @AuthenticationPrincipal
        // resuelva null (sin bloquear la request a nivel de Filter, que está excluido en esta slice),
        // igual que ocurriría con un JWT ausente/expirado en producción.
        when(casoUsoCrearResena.ejecutar(eq(idProducto), eq(null), any(SolicitudCrearResena.class)))
                .thenThrow(new ExcepcionOperacionInvalida("Debe estar autenticado para crear una reseña."));

        mockMvc.perform(post("/api/v1/productos/{idProducto}/resenas", idProducto)
                        .with(user("otro-tipo-de-principal"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(solicitudValida)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Debe estar autenticado para crear una reseña."));
    }
}
