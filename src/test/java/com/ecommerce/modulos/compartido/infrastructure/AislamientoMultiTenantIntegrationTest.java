package com.ecommerce.modulos.compartido.infrastructure;

import com.ecommerce.modulos.catalogo.domain.EstadoProducto;
import com.ecommerce.modulos.catalogo.domain.Producto;
import com.ecommerce.modulos.catalogo.domain.RepositorioProducto;
import com.ecommerce.modulos.compartido.domain.Dinero;
import com.ecommerce.modulos.identidad.domain.RepositorioUsuario;
import com.ecommerce.modulos.identidad.domain.Usuario;
import com.ecommerce.modulos.inquilino.domain.Inquilino;
import com.ecommerce.modulos.inquilino.domain.RepositorioInquilino;
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
        // SecurityConfig calls .oauth2Login(...) unconditionally, which needs a
        // ClientRegistrationRepository bean to exist to build the filter chain at all —
        // placeholder values only, the OAuth2 login flow itself isn't exercised here.
        registry.add("spring.security.oauth2.client.registration.google.client-id", () -> "test-client-id");
        registry.add("spring.security.oauth2.client.registration.google.client-secret", () -> "test-client-secret");
    }

    @Autowired
    private RepositorioProducto repositorioProducto;

    @Autowired
    private RepositorioInquilino repositorioInquilino;

    @Autowired
    private RepositorioUsuario repositorioUsuario;

    @AfterEach
    void limpiar() {
        ContextoInquilino.clear();
    }

    @Test
    void findAllSoloDevuelveProductosDelTenantActivoAunqueNoSeFiltrePorIdTiendaExplicitamente() {
        // productos.tenant_id -> inquilinos(id) -> usuarios(id) son foreign keys reales
        // (ver V3/V4 migrations), asi que hacen falta filas reales, no UUIDs sueltos.
        UUID idTiendaA = crearInquilinoConPropietario("propietario-a@test.com", "tienda-a").getId();
        UUID idTiendaB = crearInquilinoConPropietario("propietario-b@test.com", "tienda-b").getId();

        repositorioProducto.save(crearProducto(idTiendaA, "producto-tienda-a"));
        repositorioProducto.save(crearProducto(idTiendaB, "producto-tienda-b"));
        repositorioProducto.flush();

        ContextoInquilino.setIdTienda(idTiendaA);
        List<Producto> visiblesParaA = repositorioProducto.findAll();

        assertEquals(1, visiblesParaA.size());
        assertEquals(idTiendaA, visiblesParaA.get(0).getIdTienda());
    }

    private Inquilino crearInquilinoConPropietario(String correo, String enlaceCorto) {
        Usuario propietario = new Usuario(correo, "hash-no-usado-en-este-test", "Test", "Propietario");
        propietario = repositorioUsuario.save(propietario);

        Inquilino inquilino = new Inquilino("Tienda de prueba", enlaceCorto, propietario.getId());
        return repositorioInquilino.save(inquilino);
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
