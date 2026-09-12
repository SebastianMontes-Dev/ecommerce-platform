package com.ecommerce.modulos.catalogo.application;

import com.ecommerce.modulos.catalogo.domain.*;
import com.ecommerce.modulos.compartido.domain.Dinero;
import com.ecommerce.modulos.inquilino.domain.Inquilino;
import com.ecommerce.modulos.inquilino.domain.RepositorioInquilino;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reproduce el N+1 de {@code CasoUsoObtenerProducto.listProducts}: sin fetch plan, listar
 * una página de productos disparaba 1 query extra por producto para {@code categoria},
 * {@code variants} e {@code images}. Con {@code @EntityGraph} (categoria) y
 * {@code @BatchSize} (variants/images) el número de queries deja de escalar linealmente
 * con la cantidad de productos listados.
 */
@SpringBootTest(classes = com.ecommerce.bootstrap.AplicacionEcommerce.class)
@Testcontainers
class ListadoCatalogoNMasUnoIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
        registry.add("spring.jpa.properties.hibernate.generate_statistics", () -> "true");
    }

    @Autowired private RepositorioProducto repositorioProducto;
    @Autowired private RepositorioInquilino repositorioInquilino;
    @Autowired private EntityManagerFactory entityManagerFactory;
    @Autowired private PlatformTransactionManager transactionManager;

    @Test
    void listarProductosNoDebeEscalarLinealConLaCantidadDeProductos() {
        Inquilino inquilino = repositorioInquilino.save(
                new Inquilino("Tienda N+1", "tienda-nmas1-" + UUID.randomUUID(), UUID.randomUUID()));
        UUID idTienda = inquilino.getId();

        // Se deja `categoria` en null para no acoplar este test al ciclo de vida de
        // RepositorioCategoria: el N+1 de variants/images ya alcanza para demostrar el
        // problema y el fix (ver brief de la Task 1).
        for (int i = 0; i < 15; i++) {
            Producto producto = new Producto();
            producto.setIdTienda(idTienda);
            producto.setNombre("Producto " + i);
            producto.setEnlaceCorto("producto-" + i + "-" + UUID.randomUUID());
            producto.setPrecio(Dinero.of(new BigDecimal("10.00"), "USD"));
            producto.setEstado(EstadoProducto.ACTIVE);
            producto = repositorioProducto.save(producto);

            VarianteProducto variante = new VarianteProducto();
            variante.setIdTienda(idTienda);
            variante.setNombre("Única");
            variante.setMonto(new BigDecimal("10.00"));
            variante.setMoneda("USD");
            variante.setProducto(producto);
            variante.setIdProducto(producto.getId());
            producto.getVariants().add(variante);
            repositorioProducto.save(producto);
        }

        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        Statistics stats = sessionFactory.getStatistics();
        stats.clear();

        // El setup de arriba corrió fuera de una transacción explícita (cada save() de
        // Spring Data abre y cierra la suya), así que no comparte persistence-context con
        // la lectura de abajo: el identity-map no puede esconder el N+1 real.
        //
        // findAllByIdTienda es un método derivado de Spring Data — su propia transacción
        // readOnly se cierra apenas retorna, así que acceder a `variants`/`images` (LAZY)
        // después, sin sesión de Hibernate abierta, dispara LazyInitializationException.
        // Se envuelve explícitamente la fase de lectura en una transacción (mismo patrón
        // que @Transactional(readOnly = true) en CasoUsoObtenerProducto.listProducts) para
        // que la sesión siga abierta mientras se recorren las colecciones lazy.
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setReadOnly(true);
        transactionTemplate.executeWithoutResult(status ->
                repositorioProducto.findAllByIdTienda(idTienda, PageRequest.of(0, 20))
                        .forEach(p -> {
                            if (p.getCategoria() != null) p.getCategoria().getNombre();
                            p.getVariants().size();
                            p.getImages().size();
                        }));

        long queries = stats.getPrepareStatementCount();
        // Sin el fix: ~1 (count) + 1 (page) + 15 (variants) + 15 (images) = 32+.
        // Con el fix: 1 (count) + 1 (page) + 1 (batch variants) + 1 (batch images) = 4,
        // con margen para variación de Hibernate. El umbral <= 8 no pretende ser exacto:
        // lo que importa es demostrar que el conteo NO escala linealmente con los 15
        // productos listados (O(1) adicional, no O(N)).
        assertTrue(queries <= 8,
                "Se esperaban <=8 queries listando 15 productos (evitando 1-por-producto); se ejecutaron: " + queries);
    }
}
