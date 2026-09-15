package com.ecommerce.modulos.analiticas.infrastructure;

import com.ecommerce.modulos.analiticas.application.CasoUsoAnaliticas;
import com.ecommerce.modulos.analiticas.application.dto.ProductoTop;
import com.ecommerce.modulos.analiticas.application.dto.ResumenDashboard;
import com.ecommerce.modulos.analiticas.application.dto.VentaDiaria;
import com.ecommerce.modulos.compartido.infrastructure.ContextoInquilino;
import com.ecommerce.modulos.compartido.infrastructure.FiltroInquilino;
import com.ecommerce.modulos.compartido.infrastructure.InterceptorLimiteTasa;
import com.ecommerce.modulos.compartido.infrastructure.RateLimitConfig;
import com.ecommerce.modulos.compartido.infrastructure.security.FiltroAutenticacionJwt;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Mismo patrón que ControladorPagoTest/ControladorLogisticaTest: se excluyen los
// Filter/HandlerInterceptor/WebMvcConfigurer globales que @WebMvcTest detecta
// project-wide. SecurityConfig (con @EnableMethodSecurity) no se carga en este
// slice, así que el @PreAuthorize("hasRole('SELLER')") a nivel de clase no se
// aplica aquí — solo se verifica que el request llegue autenticado.
@WebMvcTest(controllers = ControladorAnaliticas.class, excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {FiltroInquilino.class, FiltroAutenticacionJwt.class,
                InterceptorLimiteTasa.class, RateLimitConfig.class}
))
@AutoConfigureMockMvc
@ContextConfiguration(classes = com.ecommerce.bootstrap.AplicacionEcommerce.class)
class ControladorAnaliticasTest {

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
    private CasoUsoAnaliticas casoUsoAnaliticas;

    private UUID idTienda;

    @BeforeEach
    void setUp() {
        idTienda = UUID.randomUUID();
    }

    @AfterEach
    void limpiarContexto() {
        ContextoInquilino.clear();
    }

    @Test
    void debeDevolver200ConElResumenCompletoDelDashboard() throws Exception {
        ContextoInquilino.setIdTiendaPropia(idTienda);
        UUID idProducto = UUID.randomUUID();
        ResumenDashboard resumen = ResumenDashboard.builder()
                .ventasTotalesMes(new BigDecimal("1000.00"))
                .ordenesTotalesMes(4)
                .ticketPromedio(new BigDecimal("250.00"))
                .ingresosUltimos7Dias(List.of(new VentaDiaria("2026-09-01", new BigDecimal("300.00"), 1)))
                .productosMasVendidos(List.of(new ProductoTop(idProducto, "Zapatilla", 10)))
                .build();

        when(casoUsoAnaliticas.obtenerResumen(idTienda)).thenReturn(resumen);

        mockMvc.perform(get("/api/v1/analiticas/dashboard")
                        .with(user("vendedor").roles("SELLER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ventasTotalesMes").value(1000.00))
                .andExpect(jsonPath("$.ordenesTotalesMes").value(4))
                .andExpect(jsonPath("$.ticketPromedio").value(250.00))
                .andExpect(jsonPath("$.ingresosUltimos7Dias.length()").value(1))
                .andExpect(jsonPath("$.productosMasVendidos[0].nombreProducto").value("Zapatilla"));

        verify(casoUsoAnaliticas).obtenerResumen(idTienda);
    }

    @Test
    void debeDevolver200ConValoresVaciosCuandoNoHayVentasEnElMes() throws Exception {
        ContextoInquilino.setIdTiendaPropia(idTienda);
        ResumenDashboard resumenVacio = ResumenDashboard.builder()
                .ventasTotalesMes(null)
                .ordenesTotalesMes(0)
                .ticketPromedio(null)
                .ingresosUltimos7Dias(List.of())
                .productosMasVendidos(List.of())
                .build();

        when(casoUsoAnaliticas.obtenerResumen(idTienda)).thenReturn(resumenVacio);

        mockMvc.perform(get("/api/v1/analiticas/dashboard")
                        .with(user("vendedor").roles("SELLER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ventasTotalesMes").doesNotExist())
                .andExpect(jsonPath("$.ordenesTotalesMes").value(0))
                .andExpect(jsonPath("$.ingresosUltimos7Dias.length()").value(0))
                .andExpect(jsonPath("$.productosMasVendidos.length()").value(0));
    }

    @Test
    void debeDevolver401CuandoElUsuarioNoTieneTiendaPropia() throws Exception {
        // No se llama a ContextoInquilino.setIdTiendaPropia(...): simula un usuario
        // autenticado sin tienda propia resuelta server-side.
        mockMvc.perform(get("/api/v1/analiticas/dashboard")
                        .with(user("vendedor").roles("SELLER")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Unauthorized"));
    }

}
