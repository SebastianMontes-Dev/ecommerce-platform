package com.ecommerce.modulos.catalogo.application;

import com.ecommerce.modulos.catalogo.domain.EstadoProducto;
import com.ecommerce.modulos.catalogo.domain.Producto;
import com.ecommerce.modulos.catalogo.domain.RepositorioProducto;
import com.ecommerce.modulos.compartido.domain.Dinero;
import com.ecommerce.modulos.compartido.domain.ExcepcionStockInsuficiente;
import com.ecommerce.modulos.compartido.infrastructure.ContextoInquilino;
import com.ecommerce.modulos.identidad.domain.RepositorioUsuario;
import com.ecommerce.modulos.identidad.domain.Usuario;
import com.ecommerce.modulos.inquilino.domain.Inquilino;
import com.ecommerce.modulos.inquilino.domain.RepositorioInquilino;
import com.ecommerce.modulos.ordenes.domain.ArticuloOrden;
import com.ecommerce.modulos.ordenes.domain.events.EventoOrdenCreada;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Escenario "último producto": N compradores intentan reservar simultáneamente la única
 * unidad en stock. El bloqueo pesimista de {@code ManejadorEventosOrden} debe garantizar
 * que exactamente uno lo consigue y el resto recibe {@link ExcepcionStockInsuficiente},
 * sin que el inventario quede negativo.
 */
@SpringBootTest(classes = com.ecommerce.bootstrap.AplicacionEcommerce.class)
@Testcontainers
class ReservaInventarioConcurrenciaIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
        registry.add("app.outbox.intervalo-ms", () -> "3600000");
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        registry.add("spring.security.oauth2.client.registration.google.client-id", () -> "test-client-id");
        registry.add("spring.security.oauth2.client.registration.google.client-secret", () -> "test-client-secret");
    }

    @Autowired private ManejadorEventosOrden manejadorEventosOrden;
    @Autowired private RepositorioProducto repositorioProducto;
    @Autowired private RepositorioInquilino repositorioInquilino;
    @Autowired private RepositorioUsuario repositorioUsuario;

    private UUID idTienda;
    private UUID idProducto;

    @AfterEach
    void limpiar() {
        ContextoInquilino.clear();
        if (idProducto != null) repositorioProducto.deleteById(idProducto);
        if (idTienda != null) repositorioInquilino.deleteById(idTienda);
    }

    @Test
    void soloUnaDeNReservasConcurrentesGanaLaUltimaUnidad() throws InterruptedException {
        int compradores = 10;
        prepararProductoConStock(1);

        ExecutorService pool = Executors.newFixedThreadPool(compradores);
        CountDownLatch listos = new CountDownLatch(compradores);
        CountDownLatch salida = new CountDownLatch(1);
        AtomicInteger exitos = new AtomicInteger();
        AtomicInteger sinStock = new AtomicInteger();
        AtomicInteger otrosErrores = new AtomicInteger();

        for (int i = 0; i < compradores; i++) {
            pool.submit(() -> {
                listos.countDown();
                try {
                    salida.await();
                    ContextoInquilino.setIdTienda(idTienda);
                    manejadorEventosOrden.handle(eventoCompraDeUnaUnidad());
                    exitos.incrementAndGet();
                } catch (ExcepcionStockInsuficiente e) {
                    sinStock.incrementAndGet();
                } catch (Exception e) {
                    otrosErrores.incrementAndGet();
                } finally {
                    ContextoInquilino.clear();
                }
            });
        }

        listos.await();
        salida.countDown(); // arranque simultáneo
        pool.shutdown();
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS), "las reservas no terminaron a tiempo");

        assertEquals(0, otrosErrores.get(), "no debería haber errores inesperados");
        assertEquals(1, exitos.get(), "exactamente una reserva debe tener éxito");
        assertEquals(compradores - 1, sinStock.get(), "el resto debe recibir ExcepcionStockInsuficiente");
        assertEquals(0, repositorioProducto.findById(idProducto).orElseThrow().getInventario(),
                "el inventario final debe ser 0, nunca negativo");
    }

    private void prepararProductoConStock(int stock) {
        Usuario propietario = repositorioUsuario.save(
                new Usuario("dueno-" + UUID.randomUUID() + "@test.com", "hash", "Test", "Dueno"));
        Inquilino inquilino = repositorioInquilino.save(
                new Inquilino("Tienda concurrencia", "tienda-conc-" + UUID.randomUUID().toString().substring(0, 8), propietario.getId()));
        idTienda = inquilino.getId();

        Producto producto = new Producto();
        producto.setIdTienda(idTienda);
        producto.setNombre("Última unidad");
        producto.setEnlaceCorto("ultima-unidad-" + UUID.randomUUID().toString().substring(0, 8));
        producto.setPrecio(Dinero.of(new BigDecimal("50.00"), "USD"));
        producto.setInventario(stock);
        producto.setRastreoInventarioHabilitado(true);
        producto.setEstado(EstadoProducto.ACTIVE);
        idProducto = repositorioProducto.save(producto).getId();
    }

    private EventoOrdenCreada eventoCompraDeUnaUnidad() {
        ArticuloOrden articulo = new ArticuloOrden();
        articulo.setIdProducto(idProducto);
        articulo.setCantidad(1);
        return new EventoOrdenCreada(UUID.randomUUID(), idTienda, UUID.randomUUID(), List.of(articulo));
    }
}
