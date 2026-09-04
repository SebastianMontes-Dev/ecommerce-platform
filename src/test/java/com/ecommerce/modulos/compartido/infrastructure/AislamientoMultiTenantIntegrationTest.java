package com.ecommerce.modulos.compartido.infrastructure;

import com.ecommerce.modulos.catalogo.domain.EstadoProducto;
import com.ecommerce.modulos.catalogo.domain.Producto;
import com.ecommerce.modulos.catalogo.domain.RepositorioProducto;
import com.ecommerce.modulos.compartido.domain.Dinero;
import org.junit.jupiter.api.AfterEach;
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
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = com.ecommerce.bootstrap.AplicacionEcommerce.class)
@Testcontainers
@Transactional
class AislamientoMultiTenantIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
    }

    @Autowired
    private RepositorioProducto repositorioProducto;

    @AfterEach
    void limpiar() {
        ContextoInquilino.clear();
    }

    @Test
    void findAllSoloDevuelveProductosDelTenantActivoAunqueNoSeFiltrePorIdTiendaExplicitamente() {
        UUID idTiendaA = UUID.randomUUID();
        UUID idTiendaB = UUID.randomUUID();

        repositorioProducto.save(crearProducto(idTiendaA, "producto-tienda-a"));
        repositorioProducto.save(crearProducto(idTiendaB, "producto-tienda-b"));
        repositorioProducto.flush();

        ContextoInquilino.setIdTienda(idTiendaA);
        List<Producto> visiblesParaA = repositorioProducto.findAll();

        assertEquals(1, visiblesParaA.size());
        assertEquals(idTiendaA, visiblesParaA.get(0).getIdTienda());
    }

    private Producto crearProducto(UUID idTienda, String enlaceCorto) {
        Producto producto = new Producto();
        producto.setIdTienda(idTienda);
        producto.setNombre("Producto de prueba");
        producto.setEnlaceCorto(enlaceCorto);
        producto.setPrecio(Dinero.of(new BigDecimal("10.00"), "USD"));
        producto.setEstado(EstadoProducto.DRAFT);
        return producto;
    }
}
