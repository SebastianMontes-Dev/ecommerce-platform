package com.ecommerce.modulos.resenas.infrastructure;

import com.ecommerce.modulos.compartido.domain.ExcepcionOperacionInvalida;
import com.ecommerce.modulos.compartido.infrastructure.FiltroInquilino;
import com.ecommerce.modulos.compartido.infrastructure.InterceptorLimiteTasa;
import com.ecommerce.modulos.compartido.infrastructure.RateLimitConfig;
import com.ecommerce.modulos.compartido.infrastructure.RespuestaPaginada;
import com.ecommerce.modulos.compartido.infrastructure.security.CustomOAuth2UserService;
import com.ecommerce.modulos.compartido.infrastructure.security.ManejadorExitoAutenticacionOAuth2;
import com.ecommerce.modulos.compartido.infrastructure.security.ManejadorFalloAutenticacionOAuth2;
import com.ecommerce.modulos.compartido.infrastructure.security.ProveedorTokenJwt;
import com.ecommerce.modulos.compartido.infrastructure.security.SecurityConfig;
import com.ecommerce.modulos.identidad.application.ServicioDetallesUsuarioPersonalizado;
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
import org.springframework.context.annotation.Import;
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
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.verify;

// A diferencia de otros ControladorXTest de este repo (que excluyen FiltroAutenticacionJwt
// y solo mockean el resolver de @AuthenticationPrincipal), este test SÍ carga la
// SecurityConfig real (@Import) porque el propósito explícito de parte de esta clase es
// verificar reglas permitAll concretas: GET /resenas es público (fix #23), POST sigue
// autenticado. @WebMvcTest no trae la @Configuration "SecurityConfig" a la slice por
// defecto (no implementa la interfaz legacy WebSecurityConfigurer, solo expone un @Bean
// SecurityFilterChain) — sin este @Import, Boot arma su propio SecurityFilterChain por
// defecto (deniega todo salvo login) y cualquier cambio real en el permitAll de
// SecurityConfig no se vería reflejado acá. FiltroAutenticacionJwt se deja real (no se
// excluye) para que el filtro que SecurityConfig registra con addFilterBefore exista de
// verdad; solo se mockean sus dependencias.
@WebMvcTest(controllers = ControladorResena.class, excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {FiltroInquilino.class, InterceptorLimiteTasa.class, RateLimitConfig.class}
))
@AutoConfigureMockMvc
@ContextConfiguration(classes = com.ecommerce.bootstrap.AplicacionEcommerce.class)
@Import(SecurityConfig.class)
class ControladorResenaTest {

    // @EnableWebSecurity ya se carga acá vía @Import(SecurityConfig.class), pero Spring Boot
    // solo auto-registra un AuthenticationPrincipalArgumentResolver cuando detecta
    // @EnableWebSecurity en el ApplicationContext completo (no siempre ocurre de forma
    // fiable dentro de un slice @WebMvcTest) — declararlo a mano es el fix mínimo y ya
    // probado en el resto de los ControladorXTest de este repo.
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

    // Dependencias de SecurityConfig/FiltroAutenticacionJwt, que ahora se cargan de verdad.
    @MockBean
    private ProveedorTokenJwt proveedorTokenJwt;

    @MockBean
    private ServicioDetallesUsuarioPersonalizado servicioDetallesUsuarioPersonalizado;

    @MockBean
    private CustomOAuth2UserService customOAuth2UserService;

    @MockBean
    private ManejadorExitoAutenticacionOAuth2 manejadorExitoAutenticacionOAuth2;

    @MockBean
    private ManejadorFalloAutenticacionOAuth2 manejadorFalloAutenticacionOAuth2;

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
    void debeListarResenasSinAutenticacion() throws Exception {
        RespuestaResena resena = respuestaDe(idProducto, 5, "Muy bueno");
        RespuestaPaginada<RespuestaResena> pagina = RespuestaPaginada.from(
                new PageImpl<>(List.of(resena), PageRequest.of(0, 10), 1));

        when(casoUsoConsultarResenas.listarActivasDeProducto(eq(idProducto), eq(PageRequest.of(0, 10))))
                .thenReturn(pagina);

        // GET es de cara al comprador (igual que catálogo/búsqueda) y no debe requerir login:
        // sin .with(user(...)), antes del fix #23 esto redirigía (302) a /oauth2/authorization/google.
        mockMvc.perform(get("/api/v1/productos/{idProducto}/resenas", idProducto))
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

        mockMvc.perform(get("/api/v1/productos/{idProducto}/resenas", idProducto))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void debeRequerirAutenticacionParaCrearResena() throws Exception {
        mockMvc.perform(post("/api/v1/productos/{idProducto}/resenas", idProducto)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(solicitudValida)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/oauth2/authorization/google"));

        verify(casoUsoCrearResena, never()).ejecutar(any(), any(), any());
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
}
