package com.ecommerce.modulos.resenas.infrastructure;

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
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// A diferencia de ControladorCarritoTest (que solo ejercita rutas autenticadas, donde
// da igual qué SecurityFilterChain esté activo), este test verifica reglas permitAll
// concretas. @WebMvcTest NO incluye la @Configuration "SecurityConfig" en la slice
// porque no implementa la interfaz legacy WebSecurityConfigurer (solo expone un @Bean
// SecurityFilterChain, el estilo recomendado desde Spring Security 5.7+) — sin este
// @Import, Boot registra su propio SecurityFilterChain por defecto (deniega todo salvo
// login), y cualquier cambio en el permitAll real de SecurityConfig no se vería reflejado.
// FiltroAutenticacionJwt SÍ se deja real (no se excluye) para que el filtro que
// SecurityConfig registra con addFilterBefore exista de verdad; solo se mockean sus
// dos dependencias, no el filtro en sí (un Filter mockeado no llama a filterChain.doFilter
// y colgaría cualquier request).
@WebMvcTest(controllers = ControladorResena.class, excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {FiltroInquilino.class, InterceptorLimiteTasa.class, RateLimitConfig.class}
))
@AutoConfigureMockMvc
@ContextConfiguration(classes = com.ecommerce.bootstrap.AplicacionEcommerce.class)
@Import(SecurityConfig.class)
class ControladorResenaTest {

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
    private CasoUsoConsultarResenas casoUsoConsultarResenas;

    @MockBean
    private CasoUsoCrearResena casoUsoCrearResena;

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

    @Autowired
    private ObjectMapper objectMapper;

    private UUID idProducto;
    private DetallesUsuarioPersonalizado userDetails;

    @BeforeEach
    void setUp() {
        idProducto = UUID.randomUUID();

        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setCorreo("cliente@test.com");
        usuario.setHashContrasena("password");
        usuario.setEnabled(true);
        usuario.setRoles(Set.of(RolUsuario.CUSTOMER));
        userDetails = new DetallesUsuarioPersonalizado(usuario);
    }

    @Test
    void debeListarResenasSinAutenticacion() throws Exception {
        RespuestaResena resena = new RespuestaResena(
                UUID.randomUUID(), idProducto, BigDecimal.valueOf(5), "Excelente", "Muy buen producto", LocalDateTime.now());
        when(casoUsoConsultarResenas.listarActivasDeProducto(eq(idProducto), any()))
                .thenReturn(RespuestaPaginada.from(new PageImpl<>(List.of(resena), PageRequest.of(0, 10), 1)));

        // GET es de cara al comprador (igual que catálogo/búsqueda) y no debe requerir login:
        // sin .with(user(...)) previamente redirigía (302) a /oauth2/authorization/google.
        mockMvc.perform(get("/api/v1/productos/{idProducto}/resenas", idProducto))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].titulo").value("Excelente"));
    }

    @Test
    void debeRequerirAutenticacionParaCrearResena() throws Exception {
        SolicitudCrearResena solicitud = new SolicitudCrearResena();
        solicitud.setIdOrden(UUID.randomUUID());
        solicitud.setCalificacion(5);
        solicitud.setTitulo("Excelente");
        solicitud.setComentario("Muy buen producto");

        mockMvc.perform(post("/api/v1/productos/{idProducto}/resenas", idProducto)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(solicitud)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/oauth2/authorization/google"));
    }

    @Test
    void debeCrearResenaParaUsuarioAutenticado() throws Exception {
        SolicitudCrearResena solicitud = new SolicitudCrearResena();
        solicitud.setIdOrden(UUID.randomUUID());
        solicitud.setCalificacion(5);
        solicitud.setTitulo("Excelente");
        solicitud.setComentario("Muy buen producto");

        RespuestaResena resena = new RespuestaResena(
                UUID.randomUUID(), idProducto, BigDecimal.valueOf(5), "Excelente", "Muy buen producto", LocalDateTime.now());
        when(casoUsoCrearResena.ejecutar(eq(idProducto), any(), any())).thenReturn(resena);

        mockMvc.perform(post("/api/v1/productos/{idProducto}/resenas", idProducto)
                        .with(user(userDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(solicitud)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titulo").value("Excelente"));
    }
}
