package com.ecommerce.modulos.ordenes.application;

import com.ecommerce.modulos.carrito.application.ServicioCarrito;
import com.ecommerce.modulos.carrito.domain.ArticuloCarrito;
import com.ecommerce.modulos.carrito.domain.Carrito;
import com.ecommerce.modulos.catalogo.domain.EstadoProducto;
import com.ecommerce.modulos.catalogo.domain.Producto;
import com.ecommerce.modulos.catalogo.domain.RepositorioProducto;
import com.ecommerce.modulos.compartido.domain.Dinero;
import com.ecommerce.modulos.compartido.domain.Direccion;
import com.ecommerce.modulos.compartido.infrastructure.ContextoInquilino;
import com.ecommerce.modulos.identidad.domain.RepositorioUsuario;
import com.ecommerce.modulos.identidad.domain.Usuario;
import com.ecommerce.modulos.inquilino.domain.Inquilino;
import com.ecommerce.modulos.inquilino.domain.RepositorioInquilino;
import com.ecommerce.modulos.ordenes.application.dto.SolicitudCheckout;
import com.ecommerce.modulos.ordenes.domain.Cupon;
import com.ecommerce.modulos.ordenes.domain.Orden;
import com.ecommerce.modulos.ordenes.domain.RepositorioCupon;
import com.ecommerce.modulos.ordenes.domain.RepositorioOrden;
import com.ecommerce.modulos.ordenes.domain.TipoDescuento;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Mismo escenario que {@code ReservaInventarioConcurrenciaIntegrationTest} pero para el cupón:
 * N checkouts concurrentes con el mismo código de un solo uso. Antes del lock pesimista en
 * {@link RepositorioCupon#findByIdTiendaAndCodigoForUpdate}, todos leían el cupón como válido
 * antes de que cualquiera registrara el uso -sobreventa del cupón-.
 */
@SpringBootTest(classes = com.ecommerce.bootstrap.AplicacionEcommerce.class)
@Testcontainers
class CuponConcurrenciaIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
        registry.add("app.outbox.intervalo-ms", () -> "3600000");
        registry.add("spring.security.oauth2.client.registration.google.client-id", () -> "test-client-id");
        registry.add("spring.security.oauth2.client.registration.google.client-secret", () -> "test-client-secret");
    }

    @Autowired private CasoUsoOrden casoUsoOrden;
    @Autowired private RepositorioOrden repositorioOrden;
    @Autowired private RepositorioCupon repositorioCupon;
    @Autowired private RepositorioProducto repositorioProducto;
    @Autowired private RepositorioInquilino repositorioInquilino;
    @Autowired private RepositorioUsuario repositorioUsuario;

    @MockBean
    private ServicioCarrito servicioCarrito;
    @MockBean
    private com.ecommerce.modulos.compartido.infrastructure.websocket.ServicioNotificacionTiempoReal servicioNotificacionTiempoReal;

    private UUID idTienda;
    private UUID idProducto;
    private UUID idCupon;

    @AfterEach
    void limpiar() {
        ContextoInquilino.clear();
        if (idProducto != null) repositorioProducto.deleteById(idProducto);
        if (idCupon != null) repositorioCupon.deleteById(idCupon);
        if (idTienda != null) repositorioInquilino.deleteById(idTienda);
    }

    @Test
    void soloUnCheckoutConcurrenteConsumeElCuponDeUnSoloUso() throws InterruptedException {
        int compradores = 10;
        String codigo = "SOLOUNO";
        prepararTiendaProductoYCupon(codigo);

        List<UUID> clientes = crearClientes(compradores);

        // El stub se arma UNA sola vez, antes de lanzar los hilos: llamar when(...) sobre el
        // mismo mock concurrentemente desde varios hilos no es seguro en Mockito (el registro
        // interno de stubbings no está pensado para escritura concurrente). thenAnswer además
        // devuelve un Carrito nuevo por invocación -si fuera el mismo objeto compartido,
        // agregarArticulo() en paralelo sería su propia carrera de datos-.
        when(servicioCarrito.getOrCreateCart(any(UUID.class), eq(idTienda)))
                .thenAnswer(invocation -> carritoConCuponPara(codigo));

        ExecutorService pool = Executors.newFixedThreadPool(compradores);
        CountDownLatch listos = new CountDownLatch(compradores);
        CountDownLatch salida = new CountDownLatch(1);
        AtomicInteger errores = new AtomicInteger();
        java.util.concurrent.atomic.AtomicReference<Throwable> ultimoError = new java.util.concurrent.atomic.AtomicReference<>();

        for (UUID idCliente : clientes) {
            pool.submit(() -> {
                listos.countDown();
                try {
                    salida.await();
                    ContextoInquilino.setIdTienda(idTienda);
                    casoUsoOrden.createOrderFromCart(idCliente, idTienda, solicitudCheckoutDeMuestra());
                } catch (Exception e) {
                    errores.incrementAndGet();
                    ultimoError.set(e);
                } finally {
                    ContextoInquilino.clear();
                }
            });
        }

        listos.await();
        salida.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS), "los checkouts no terminaron a tiempo");

        assertEquals(0, errores.get(), "el checkout nunca debe fallar aunque el cupón ya no sea válido: "
                + (ultimoError.get() != null ? ultimoError.get() : ""));

        List<Orden> ordenes = repositorioOrdenDeLaTienda();
        long ordenesConCupon = ordenes.stream().filter(o -> o.getCodigoCupon() != null).count();

        assertEquals(compradores, ordenes.size(), "todos los checkouts deben crear su orden");
        assertEquals(1, ordenesConCupon, "exactamente una orden debe haberse quedado con el descuento");
        assertEquals(1, repositorioCupon.findById(idCupon).orElseThrow().getUsosActuales(),
                "el cupón no debe registrar más usos que su límite");
    }

    private List<Orden> repositorioOrdenDeLaTienda() {
        return repositorioOrden.findAllByIdTienda(idTienda, org.springframework.data.domain.Pageable.unpaged()).getContent();
    }

    private List<UUID> crearClientes(int cantidad) {
        return java.util.stream.IntStream.range(0, cantidad)
                .mapToObj(i -> repositorioUsuario.save(
                        new Usuario("cliente-cupon-" + UUID.randomUUID() + "@test.com", "hash", "Cliente", "Test" + i))
                        .getId())
                .toList();
    }

    private Carrito carritoConCuponPara(String codigo) {
        ArticuloCarrito articulo = new ArticuloCarrito();
        articulo.setIdProducto(idProducto);
        articulo.setNombreProducto("Producto con cupón");
        articulo.setCantidad(1);
        articulo.setPrecioUnitario(new BigDecimal("100.00"));
        articulo.setMoneda("USD");

        Carrito carrito = Carrito.builder()
                .idTienda(idTienda)
                .codigoCupon(codigo)
                .montoDescuento(new BigDecimal("10.00"))
                .build();
        carrito.agregarArticulo(articulo);
        return carrito;
    }

    private SolicitudCheckout solicitudCheckoutDeMuestra() {
        SolicitudCheckout request = new SolicitudCheckout();
        Direccion direccion = Direccion.of("Calle 1", "Ciudad", "Provincia", "0000", "AR");
        request.setDireccionEnvio(direccion);
        request.setDireccionFacturacion(direccion);
        return request;
    }

    private void prepararTiendaProductoYCupon(String codigo) {
        Usuario propietario = repositorioUsuario.save(
                new Usuario("dueno-cupon-" + UUID.randomUUID() + "@test.com", "hash", "Test", "Dueno"));
        Inquilino inquilino = repositorioInquilino.save(new Inquilino(
                "Tienda cupón", "tienda-cupon-" + UUID.randomUUID().toString().substring(0, 8), propietario.getId()));
        idTienda = inquilino.getId();

        Producto producto = new Producto();
        producto.setIdTienda(idTienda);
        producto.setNombre("Producto con cupón");
        producto.setEnlaceCorto("producto-cupon-" + UUID.randomUUID().toString().substring(0, 8));
        producto.setPrecio(Dinero.of(new BigDecimal("100.00"), "USD"));
        producto.setInventario(10_000);
        producto.setRastreoInventarioHabilitado(true);
        producto.setEstado(EstadoProducto.ACTIVE);
        idProducto = repositorioProducto.save(producto).getId();

        Cupon cupon = new Cupon();
        cupon.setIdTienda(idTienda);
        cupon.setCodigo(codigo);
        cupon.setTipo(TipoDescuento.PORCENTAJE);
        cupon.setValor(new BigDecimal("10"));
        cupon.setLimiteUsos(1);
        cupon.setActivo(true);
        idCupon = repositorioCupon.save(cupon).getId();
    }
}
