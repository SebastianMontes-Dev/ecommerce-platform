package com.ecommerce.modulos.busqueda.application;

import com.ecommerce.modulos.catalogo.application.CasoUsoCrearProducto;
import com.ecommerce.modulos.catalogo.application.dto.SolicitudCrearProducto;
import com.ecommerce.modulos.compartido.infrastructure.outbox.EstadoEventoOutbox;
import com.ecommerce.modulos.compartido.infrastructure.outbox.ProcesadorOutbox;
import com.ecommerce.modulos.compartido.infrastructure.outbox.RepositorioEventoOutbox;
import com.ecommerce.modulos.identidad.domain.RepositorioUsuario;
import com.ecommerce.modulos.identidad.domain.Usuario;
import com.ecommerce.modulos.inquilino.domain.Inquilino;
import com.ecommerce.modulos.inquilino.domain.RepositorioInquilino;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Prueba el camino completo del outbox: crear un producto escribe la fila de outbox en la
 * misma transacción, y el worker la entrega a Elasticsearch.
 */
@SpringBootTest(classes = com.ecommerce.bootstrap.AplicacionEcommerce.class)
@ActiveProfiles("test")
@Testcontainers
class OutboxIndexacionIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @Container
    static ElasticsearchContainer elasticsearch = new ElasticsearchContainer(
            DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:8.18.0"))
            .withEnv("xpack.security.enabled", "false")
            .withEnv("discovery.type", "single-node");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        registry.add("app.elasticsearch.host", elasticsearch::getHost);
        registry.add("app.elasticsearch.port", () -> elasticsearch.getMappedPort(9200));
    }

    @Autowired private CasoUsoCrearProducto casoUsoCrearProducto;
    @Autowired private RepositorioEventoOutbox repositorioEventoOutbox;
    @Autowired private ProcesadorOutbox procesadorOutbox;
    @Autowired private ServicioBusqueda servicioBusqueda;
    @Autowired private RepositorioInquilino repositorioInquilino;
    @Autowired private RepositorioUsuario repositorioUsuario;

    @Test
    void crearProductoEscribeOutboxYElWorkerLoIndexaEnElasticsearch() {
        UUID idTienda = crearTienda();

        SolicitudCrearProducto solicitud = new SolicitudCrearProducto();
        solicitud.setNombre("Termo de acero Nexa");
        solicitud.setEnlaceCorto("termo-acero-nexa");
        solicitud.setDescripcion("Termo de acero inoxidable 1L");
        solicitud.setPrecio(new BigDecimal("29.90"));
        solicitud.setMoneda("USD");
        solicitud.setInventario(10);

        casoUsoCrearProducto.execute(solicitud, idTienda);

        // 1. La fila de outbox quedó escrita (misma transacción que el producto).
        var pendientes = repositorioEventoOutbox.findAll().stream()
                .filter(e -> e.getIdTienda().equals(idTienda))
                .toList();
        assertEquals(1, pendientes.size());
        assertEquals("BUSQUEDA_INDEXAR_PRODUCTO", pendientes.get(0).getTipo());
        assertEquals(EstadoEventoOutbox.PENDIENTE, pendientes.get(0).getEstado());

        // 2. El worker la procesa.
        procesadorOutbox.procesar();

        assertEquals(EstadoEventoOutbox.PROCESADO,
                repositorioEventoOutbox.findById(pendientes.get(0).getId()).orElseThrow().getEstado());

        // 3. El documento está en Elasticsearch (refresh no es instantáneo).
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<?> resultado = servicioBusqueda.busqueda(idTienda, "termo acero");
            assertFalse(resultado.isEmpty());
        });
    }

    private UUID crearTienda() {
        Usuario propietario = repositorioUsuario.save(
                new Usuario("dueno-" + UUID.randomUUID() + "@test.com", "hash", "Test", "Dueno"));
        Inquilino inquilino = repositorioInquilino.save(new Inquilino(
                "Tienda outbox", "tienda-outbox-" + UUID.randomUUID().toString().substring(0, 8), propietario.getId()));
        return inquilino.getId();
    }
}
