package com.ecommerce.modulos.analiticas.application;

import com.ecommerce.modulos.analiticas.application.dto.ResumenDashboard;
import com.ecommerce.modulos.compartido.domain.Dinero;
import com.ecommerce.modulos.identidad.domain.RepositorioUsuario;
import com.ecommerce.modulos.identidad.domain.Usuario;
import com.ecommerce.modulos.inquilino.domain.Inquilino;
import com.ecommerce.modulos.inquilino.domain.RepositorioInquilino;
import com.ecommerce.modulos.ordenes.domain.EstadoOrden;
import com.ecommerce.modulos.ordenes.domain.Orden;
import com.ecommerce.modulos.ordenes.domain.RepositorioOrden;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Ejecuta {@link CasoUsoAnaliticas} contra un Postgres real (esquema generado desde las
 * entidades JPA, igual que el resto de los *IntegrationTest del repo) para que las 3 queries
 * SQL crudas se validen contra columnas que existen de verdad. CasoUsoAnaliticasTest mockea
 * JdbcTemplate por completo, por lo que nunca detectó que las queries referenciaban una
 * columna ("creado_en") que no existe en la tabla "ordenes" (la real es "created_at").
 */
@SpringBootTest(classes = com.ecommerce.bootstrap.AplicacionEcommerce.class)
@Testcontainers
@Transactional
class CasoUsoAnaliticasIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
        registry.add("app.outbox.intervalo-ms", () -> "3600000");
        // SecurityConfig calls .oauth2Login(...) unconditionally, que necesita un
        // ClientRegistrationRepository para poder construir la filter chain — valores
        // de relleno nomas, el flujo de OAuth2 login no se ejercita en este test.
        registry.add("spring.security.oauth2.client.registration.google.client-id", () -> "test-client-id");
        registry.add("spring.security.oauth2.client.registration.google.client-secret", () -> "test-client-secret");
    }

    @Autowired
    private CasoUsoAnaliticas casoUsoAnaliticas;

    @Autowired
    private RepositorioOrden repositorioOrden;

    @Autowired
    private RepositorioInquilino repositorioInquilino;

    @Autowired
    private RepositorioUsuario repositorioUsuario;

    private UUID idTienda;
    private UUID idCliente;

    @BeforeEach
    void setUp() {
        idTienda = crearTienda().getId();
        idCliente = crearCliente().getId();
    }

    @Test
    void debeCalcularResumenContraElEsquemaRealDeLaTablaOrdenes() {
        guardarOrden(idTienda, idCliente, EstadoOrden.PAID, "100.00");

        ResumenDashboard resumen = casoUsoAnaliticas.obtenerResumen(idTienda);

        assertEquals(0, new BigDecimal("100.00").compareTo(resumen.getVentasTotalesMes()));
        assertEquals(1, resumen.getOrdenesTotalesMes());
        assertEquals(0, new BigDecimal("100.00").compareTo(resumen.getTicketPromedio()));

        assertEquals(1, resumen.getIngresosUltimos7Dias().size());
        assertEquals(0, new BigDecimal("100.00").compareTo(resumen.getIngresosUltimos7Dias().get(0).getMonto()));
        assertEquals(1, resumen.getIngresosUltimos7Dias().get(0).getCantidadOrdenes());
    }

    @Test
    void debeIgnorarOrdenesDeOtraTiendaYDeEstadosNoFacturables() {
        UUID idOtraTienda = crearTienda().getId();
        guardarOrden(idOtraTienda, idCliente, EstadoOrden.PAID, "999.00");
        guardarOrden(idTienda, idCliente, EstadoOrden.PENDING, "50.00");

        ResumenDashboard resumen = casoUsoAnaliticas.obtenerResumen(idTienda);

        assertEquals(0, resumen.getOrdenesTotalesMes());
        assertNull(resumen.getVentasTotalesMes());
        assertTrue(resumen.getIngresosUltimos7Dias().isEmpty());
    }

    private Inquilino crearTienda() {
        Inquilino inquilino = new Inquilino("Tienda IT " + UUID.randomUUID(), "tienda-" + UUID.randomUUID(), UUID.randomUUID());
        return repositorioInquilino.saveAndFlush(inquilino);
    }

    private Usuario crearCliente() {
        Usuario usuario = new Usuario(UUID.randomUUID() + "@test.com", "hash-irrelevante", "Cliente", "IT");
        return repositorioUsuario.saveAndFlush(usuario);
    }

    private void guardarOrden(UUID idTienda, UUID idCliente, EstadoOrden estado, String montoTotal) {
        Orden orden = new Orden();
        orden.setIdTienda(idTienda);
        orden.setNumeroOrden("IT-" + UUID.randomUUID());
        orden.setIdCliente(idCliente);
        orden.setCorreoCliente("cliente@test.com");
        orden.setSubtotal(Dinero.of(new BigDecimal(montoTotal), "USD"));
        orden.setMontoImpuesto(Dinero.of(BigDecimal.ZERO, "USD"));
        orden.setMontoEnvio(Dinero.of(BigDecimal.ZERO, "USD"));
        orden.setTotal(Dinero.of(new BigDecimal(montoTotal), "USD"));
        orden.setMontoDescuento(Dinero.of(BigDecimal.ZERO, "USD"));
        orden.setEstado(estado);
        repositorioOrden.saveAndFlush(orden);
    }
}
