# Fase 9 — Consistencia de API: listado sin N+1 y búsqueda real

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Cerrar la Fase 9 de la ETAPA 2 (auditoría de continuación de NexaSaaS, sesión 2026-09-11): el listado paginado de catálogo dispara N+1 queries por categoría/variantes/imágenes, el endpoint de búsqueda acepta `categoria/minPrice/maxPrice/minRating/sort/page/size` pero solo usa `q` (y miente `"totalPages": 1` siempre), `ControladorCupon` devuelve la entidad JPA `Cupon` directo al cliente, y varios DTOs de escritura (`SolicitudCrearProducto`, `SolicitudCrearResena`, `SolicitudRegistro`, `SolicitudCrearCupon`) no acotan el tamaño de sus campos de texto ni el rango de negocio del descuento porcentual — un valor gigante o un `valor=500` con `tipo=PORCENTAJE` llega hasta Postgres o queda en el dominio antes de fallar.

**Architecture:** No hay cambios de arquitectura. Se corrige comportamiento dentro de las capas ya establecidas (`ControladorX` → `CasoUsoX`/`ServicioX` → `RepositorioX`): 1) `@EntityGraph` en la query paginada de catálogo para la asociación `ManyToOne` (`categoria`) + `@BatchSize` en las colecciones `LAZY` (`variants`, `images`) para convertir N+1 en O(1) queries adicionales por página (no se puede `JOIN FETCH` dos colecciones simultáneamente paginadas — `MultipleBagFetchException` — así que la vía correcta es batch fetching, no un fetch join); 2) `ServicioBusqueda.busqueda` pasa a aceptar y aplicar de verdad los filtros/paginación/orden que ya declara la firma pública de `ControladorBusqueda`, y a devolver el total real de Elasticsearch; 3) nuevo DTO `RespuestaCupon` siguiendo el mismo patrón que `RespuestaResena`/`RespuestaProducto` (Fase 3 ya hizo esto mismo para logística/reseñas); 4) anotaciones `@Size`/`@AssertTrue` de Bean Validation en los DTOs existentes.

**Tech Stack:** Java 21, Spring Boot 3.4.4, Spring Data JPA (Hibernate 6), `co.elastic.clients:elasticsearch-java:8.18.0`, Jakarta Bean Validation, JUnit 5 + Mockito + Testcontainers (Postgres).

**Spec:** Este plan documenta su propio spec — surge de la ETAPA 2 de la auditoría de continuación de NexaSaaS (sesión 2026-09-11), Fase 9 del roadmap ahí definido. Cada tarea cita el hallazgo con archivo:línea verificado contra el código real (no contra documentación) con CodeGraph antes de escribir este plan.

## Global Constraints

- Mantener la convención de nombres en español del proyecto (`ServicioX`, `CasoUsoX`, `ControladorX`, `RepositorioX`, DTOs `SolicitudX`/`RespuestaX`) — ver `CLAUDE.md`.
- No introducir dependencias nuevas de gestor de paquetes — todo lo necesario ya está en `build.gradle`.
- No modificar el contrato JSON de endpoints ya usados por otros módulos salvo lo que cada tarea pide explícitamente (p. ej. `ControladorCupon` deja de devolver la entidad, pero los mismos campos siguen presentes en `RespuestaCupon`).
- Commits en español, Conventional Commits (`fix`, `perf`, `refactor` según corresponda), uno por tarea completada — sin `Co-Authored-By` de ningún asistente de IA (regla absoluta del proyecto, ver `CLAUDE.md` y `CONTRIBUTING.md`).
- `./gradlew test` es el comando de verificación; los `*IntegrationTest` usan Testcontainers (Docker) — si Docker no está disponible en el entorno de ejecución, documentarlo en el reporte de la tarea y apoyarse en los tests unitarios/`@DataJpaTest` para esa tarea, pero el test de integración debe quedar escrito y compilando igual.
- No tocar `RepositorioProducto.findAllByIdTiendaAndEstado` ni `findAllByIdTiendaAndIdCategoria` salvo que una tarea lo pida: no tienen callers hoy y están fuera de alcance de esta fase.

---

### Task 1: Eliminar el N+1 del listado paginado de catálogo

**Contexto del hallazgo:** `CasoUsoObtenerProducto.listProducts` (`src/main/java/com/ecommerce/modulos/catalogo/application/CasoUsoObtenerProducto.java:45-49`) llama a `RepositorioProducto.findAllByIdTienda(idTienda, pageable)` (`RepositorioProducto.java:27`, sin fetch alguno) y mapea cada `Producto` con `CasoUsoCrearProducto.mapToResponse` (`CasoUsoCrearProducto.java:85-127`), que accede a `producto.getCategoria().getNombre()` (asociación `@ManyToOne(fetch = LAZY)`, `Producto.java:75-77`), `producto.getVariants()` y `producto.getImages()` (colecciones `@OneToMany(fetch = LAZY)`, `Producto.java:79-84`). Para una página de 20 productos esto dispara hasta 1 (page) + 20×3 = 61 queries. `findAllByIdTienda(UUID, Pageable)` en `RepositorioProducto` no tiene otro caller (confirmado con `codegraph_callers` — único uso es este), así que se puede anotar sin efectos colaterales.

**Files:**
- Modify: `src/main/java/com/ecommerce/modulos/catalogo/domain/RepositorioProducto.java`
- Modify: `src/main/java/com/ecommerce/modulos/catalogo/domain/Producto.java`
- Test: `src/test/java/com/ecommerce/modulos/catalogo/application/ListadoCatalogoNMasUnoIntegrationTest.java` (nuevo)

**Interfaces:**
- No cambia ninguna firma pública. `RepositorioProducto.findAllByIdTienda(UUID, Pageable) -> Page<Producto>` mantiene su contrato; solo cambia el plan de fetch.

- [ ] **Step 1: Escribir el test de integración que falla (cuenta de queries)**

Usa el mismo patrón de `ReservaInventarioConcurrenciaIntegrationTest.java` (Testcontainers, `@SpringBootTest(classes = com.ecommerce.bootstrap.AplicacionEcommerce.class)`, `PostgreSQLContainer`, `@DynamicPropertySource` con `ddl-auto=update`). Este test **no** necesita el contenedor de Redis (no lo agregues).

Nota importante sobre `VarianteProducto`: la columna `product_id` está mapeada dos veces — `idProducto` (`@Column`, escribible) y `producto` (`@ManyToOne`, `insertable = false, updatable = false`, de solo lectura). Para que la variante quede realmente asociada al producto al persistir, hay que setear **ambos**: `variante.setProducto(producto)` (para la asociación en memoria) y `variante.setIdProducto(producto.getId())` (para que Hibernate escriba la FK) — esto requiere guardar el producto primero para obtener su id antes de crear/guardar la variante.

```java
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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void listarProductosNoDebeEscalarLinealConLaCantidadDeProductos() {
        Inquilino inquilino = repositorioInquilino.save(
                new Inquilino("Tienda N+1", "tienda-nmas1-" + UUID.randomUUID(), UUID.randomUUID()));
        UUID idTienda = inquilino.getId();

        Categoria categoria = new Categoria();
        categoria.setIdTienda(idTienda);
        categoria.setNombre("Calzado");
        categoria.setEnlaceCorto("calzado");
        // usar el repositorio de Categoria si hace falta guardarla explícitamente antes de
        // referenciarla por id en cada Producto (ver RepositorioCategoria)

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

        repositorioProducto.findAllByIdTienda(idTienda, PageRequest.of(0, 20))
                .forEach(p -> {
                    if (p.getCategoria() != null) p.getCategoria().getNombre();
                    p.getVariants().size();
                    p.getImages().size();
                });

        long queries = stats.getPrepareStatementCount();
        // Sin el fix: ~1 (count) + 1 (page) + 15 (categoria) + 15 (variants) + 15 (images) = 47+.
        // Con el fix: 1 (count) + 1 (page con categoria via EntityGraph) + 1 (batch variants)
        // + 1 (batch images) = 4, con margen para variación de Hibernate.
        assertTrue(queries <= 8,
                "Se esperaban <=8 queries listando 15 productos (evitando 1-por-producto); se ejecutaron: " + queries);
    }
}
```

Ajustar la creación de `Categoria` según el repositorio real (`RepositorioCategoria`, si existe con ese nombre — verificar con `codegraph_search` antes de escribir el test final) y setear `idCategoria` en cada `Producto` si se decide ejercitar también el fetch de categoría; si añadir `Categoria` complica el test, es válido dejar `categoria` en `null` para esta prueba (el N+1 de `variants`/`images` ya es suficiente para demostrar el problema y el fix) — usar criterio.

- [ ] **Step 2: Correr el test y verificar que falla**

Run: `./gradlew test --tests "com.ecommerce.modulos.catalogo.application.ListadoCatalogoNMasUnoIntegrationTest"`
Expected: FAIL — el conteo de `PrepareStatementCount` supera el límite (N+1 sin corregir). Si Docker no está disponible en este entorno, documentarlo en el reporte y seguir con el Step 3 igual (el test debe compilar y quedar listo para CI).

- [ ] **Step 3: Aplicar `@EntityGraph` en la query paginada**

En `RepositorioProducto.java`, agregar el import `org.springframework.data.jpa.repository.EntityGraph` y anotar:

```java
@EntityGraph(attributePaths = {"categoria"})
Page<Producto> findAllByIdTienda(UUID idTienda, Pageable pageable);
```

- [ ] **Step 4: Agregar `@BatchSize` a las colecciones LAZY de `Producto`**

En `Producto.java`, agregar el import `org.hibernate.annotations.BatchSize` y anotar ambas colecciones:

```java
@BatchSize(size = 20)
@OneToMany(mappedBy = "producto", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
private List<VarianteProducto> variants = new ArrayList<>();

@BatchSize(size = 20)
@OneToMany(mappedBy = "producto", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
@OrderBy("sortOrder ASC")
private List<ImagenProducto> images = new ArrayList<>();
```

- [ ] **Step 5: Correr el test y verificar que pasa**

Run: `./gradlew test --tests "com.ecommerce.modulos.catalogo.application.ListadoCatalogoNMasUnoIntegrationTest"`
Expected: PASS.

- [ ] **Step 6: Correr la suite completa de catálogo para descartar regresiones**

Run: `./gradlew test --tests "com.ecommerce.modulos.catalogo.*"`
Expected: todos los tests existentes siguen en verde (en particular los que ya cubren `bySlug`/`byId`, que usan un fetch distinto y no deben verse afectados).

---

### Task 2: Búsqueda real en Elasticsearch (paginación, filtros y total reales)

> **Actualización post-revisión final (ruling, ver `.superpowers/sdd/2026-09-11-fase-9-consistencia-api-listado-busqueda/progress.md`):** los requisitos 5 y parte del 6 de abajo (filtro `minRating`, orden `sort=rating`) quedaron **descopados**. La revisión final de branch encontró que `calificacionPromedio` (el campo sobre el que operarían) no lo escribe ningún código del repo — ni al crear/actualizar un producto ni al crear una reseña — así que ambos garantizaban 0 resultados o un orden arbitrario. Implementar la agregación real (reseñas → promedio → reindexar en Elasticsearch) es una feature nueva que cruza el módulo `resenas`, fuera de alcance de este fix wave; queda para una fase de seguimiento. `ServicioBusqueda.busqueda`/`ControladorBusqueda.searchProducts` ya no aceptan `minRating`.

**Contexto del hallazgo:** `ControladorBusqueda.searchProducts` (`src/main/java/com/ecommerce/modulos/busqueda/infrastructure/ControladorBusqueda.java:26-56`) acepta `categoria`, `minPrice`, `maxPrice`, `minRating`, `sort`, `page`, `size` pero solo pasa `q` a `ServicioBusqueda.busqueda(UUID, String)` (`ServicioBusqueda.java:41-63`); el resto de los parámetros se loguea y se devuelve tal cual en el bloque `"filters"` sin aplicarse nunca a la query de Elasticsearch. Además `"totalPages": 1` y `"totalElements": results.size()` están hardcodeados/derivados del tamaño de la lista en memoria, no del total real que devuelve Elasticsearch — un cliente que pagina nunca puede saber si hay más resultados.

**Files:**
- Modify: `src/main/java/com/ecommerce/modulos/busqueda/application/ServicioBusqueda.java`
- Modify: `src/main/java/com/ecommerce/modulos/busqueda/infrastructure/ControladorBusqueda.java`
- Test: `src/test/java/com/ecommerce/modulos/busqueda/application/ServicioBusquedaTest.java` (extender el existente, mismo patrón de mocks ya usado ahí con `elasticsearchClient.search(any(Function.class), eq(DocumentoProducto.class))`)

**Interfaces:**
- Produces: `ServicioBusqueda.busqueda(UUID idTienda, String query, String categoria, BigDecimal minPrice, BigDecimal maxPrice, Double minRating, String sort, int page, int size) -> ResultadoBusqueda` — nuevo record/clase `ResultadoBusqueda(List<DocumentoProducto> content, long totalElements)` en el mismo paquete (`busqueda.application` o `busqueda.domain`, a criterio).
- Consumes: `ControladorBusqueda.searchProducts` pasa a llamar a la nueva firma con todos los parámetros que ya recibe por `@RequestParam`, y calcula `totalPages` como `(int) Math.ceil((double) resultado.totalElements() / size)` (mínimo 1 si `size <= 0` no aplica, `size` siempre viene con `defaultValue = "20"`).

**Requisitos exactos (contrato a cumplir, no hay código de referencia completo — la API de `co.elastic.clients:elasticsearch-java:8.18.0` requiere verificar contra el compilador, no copiar a ciegas):**

1. `query` (el actual parámetro `q`) sigue siendo opcional: si es `null`/blank, no debe aplicarse el `multiMatch` (hoy pasar `query=null` al `multiMatch` actual probablemente ya rompe o matchea todo — comprobarlo y decidir).
2. Paginación real: `from = page * size`, `size = size`, en el request a Elasticsearch (métodos `.from(int)`/`.size(int)` del builder de `SearchRequest`).
3. Filtro `categoria`: term match exacto contra el sub-campo `nombreCategoria.keyword` del documento (mismo patrón que el filtro existente `idTienda.keyword`) cuando `categoria` no es blank.
4. Filtros `minPrice`/`maxPrice`: range query sobre el campo `precio` (`gte`/`lte` según cuáles vengan no-nulos).
5. Filtro `minRating`: range query `gte` sobre `calificacionPromedio`.
6. `sort`: al menos soportar `"price_asc"`, `"price_desc"` y `"rating"` (orden explícito por `precio`/`calificacionPromedio`); cualquier otro valor (incluido el default `"relevance"`) deja el orden por relevancia de Elasticsearch (no seteás `.sort(...)`).
7. El total devuelto en la respuesta debe ser `response.hits().total().value()` de Elasticsearch, no `content.size()`.
8. Si Elasticsearch falla (mismo `catch (Exception e)` que ya existe), devolver `ResultadoBusqueda` vacío con `totalElements = 0` — mantener el comportamiento actual de no propagar la excepción en búsqueda (a diferencia de `indexProduct`/`deleteProduct`, que sí propagan desde la Fase 4/Outbox).
9. `ControladorCupon` — no, esta tarea no lo toca; ver Task 3. `ControladorBusqueda.searchProducts` deja de aceptar/ignorar parámetros: todos los que declara su firma deben terminar afectando la query o la respuesta.

- [ ] **Step 1: Extender `ServicioBusquedaTest` con los casos que fallan**

Agregar (como mínimo) estos tests al archivo existente, siguiendo el patrón ya usado ahí (mock de `elasticsearchClient.search`, `ArgumentCaptor<Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>>` o inspección del `SearchRequest` construido si el captor de `Function` no permite inspeccionar el resultado fácilmente — usar `SearchRequest.Builder` real y capturar el objeto construido, no solo verificar que se llamó):

- `debeAplicarPaginacionRealFromYSize()`: llama con `page=2, size=10` y verifica que el `SearchRequest` capturado tiene `from=20` y `size=10`.
- `debeDevolverElTotalRealDeElasticsearchNoElTamanioDeLaListaDeHits()`: mockear una respuesta con `hits().total().value() = 137` pero solo 10 hits en la página, y verificar que `ResultadoBusqueda.totalElements() == 137` (no `10`).
- `debeAplicarFiltroDeCategoriaCuandoSeProvee()`: verificar que, con `categoria="Calzado"`, el bool query construido incluye un filtro/must sobre `nombreCategoria.keyword` con valor `"Calzado"`.
- `debeAplicarFiltroDeRangoDePrecioCuandoSeProveenMinYMaxPrice()`.
- `debeOrdenarPorPrecioAscendenteCuandoSortEsPriceAsc()`.
- `debeIgnorarFiltrosOpcionalesCuandoNoSeProveen()`: con todos los filtros en `null` y `query=null`, no debe romper (regresión del comportamiento actual con solo `idTienda` + `query`).

No es obligatorio que los nombres sean exactamente estos, pero deben cubrir cada uno de los 6 requisitos numerados arriba.

- [ ] **Step 2: Correr los tests nuevos y verificar que fallan**

Run: `./gradlew test --tests "com.ecommerce.modulos.busqueda.application.ServicioBusquedaTest"`
Expected: FAIL (el método actual no acepta esos parámetros — no compila todavía, lo cual es la señal correcta de "test que falla" en este caso).

- [ ] **Step 3: Implementar la nueva firma de `ServicioBusqueda.busqueda`**

Reescribir `busqueda` con la firma y el `ResultadoBusqueda` descritos en Interfaces, aplicando los 9 requisitos. Mantener el filtro obligatorio `idTienda.keyword` tal cual está. Iterar contra `./gradlew compileJava` para resolver la API exacta del cliente 8.18.0 (`Query.of`, `RangeQuery`/`NumberRangeQuery`, `SortOptions`, etc.) — no hay una única forma "correcta", cualquier construcción que cumpla los 9 requisitos y compile es válida.

- [ ] **Step 4: Actualizar `ControladorBusqueda.searchProducts`**

Pasar todos los `@RequestParam` ya declarados a la nueva firma de `busqueda`, y construir la respuesta con `totalElements`/`totalPages` reales (`Math.ceil`) en vez de los valores hardcodeados.

- [ ] **Step 5: Correr los tests y verificar que pasan**

Run: `./gradlew test --tests "com.ecommerce.modulos.busqueda.*"`
Expected: PASS, incluido `OutboxIndexacionIntegrationTest` si toca Docker (documentar si no corre por falta de Docker).

---

### Task 3: `ControladorCupon` deja de exponer la entidad JPA `Cupon`

**Contexto del hallazgo:** `ControladorCupon.crearCupon` devuelve `ResponseEntity<Cupon>` y `listarCupones` devuelve `ResponseEntity<RespuestaPaginada<Cupon>>` (`src/main/java/com/ecommerce/modulos/ordenes/infrastructure/ControladorCupon.java:31,38`), y `CasoUsoGestionarCupon.crearCupon`/`listarCupones` (`src/main/java/com/ecommerce/modulos/ordenes/application/CasoUsoGestionarCupon.java:26-48`) devuelven `Cupon`/`RespuestaPaginada<Cupon>` directamente — la entidad JPA (`extends EntidadInquilino`, con `tenant_id`, columnas de auditoría, etc.) serializa tal cual al cliente. Es la misma clase de problema que la Fase 3 ya resolvió para `ControladorLogistica`/`ControladorResena` (ver `RespuestaResena` como precedente exacto en este mismo repo).

**Files:**
- Create: `src/main/java/com/ecommerce/modulos/ordenes/application/dto/RespuestaCupon.java`
- Modify: `src/main/java/com/ecommerce/modulos/ordenes/application/CasoUsoGestionarCupon.java`
- Modify: `src/main/java/com/ecommerce/modulos/ordenes/infrastructure/ControladorCupon.java`
- Test: `src/test/java/com/ecommerce/modulos/ordenes/application/CasoUsoGestionarCuponTest.java` (extender el existente)

**Interfaces:**
- Produces: `RespuestaCupon` (record-like DTO, ver código abajo). `CasoUsoGestionarCupon.crearCupon(UUID, SolicitudCrearCupon) -> RespuestaCupon` (antes `-> Cupon`). `CasoUsoGestionarCupon.listarCupones(UUID, Pageable) -> RespuestaPaginada<RespuestaCupon>` (antes `RespuestaPaginada<Cupon>`).
- No cambia: `validarYObtenerCupon` y `calcularDescuento` — esos siguen devolviendo/usando `Cupon` internamente (son de uso interno del módulo `ordenes`, no se exponen vía `ControladorCupon`); no tocarlos.

- [ ] **Step 1: Escribir el DTO `RespuestaCupon`**

```java
package com.ecommerce.modulos.ordenes.application.dto;

import com.ecommerce.modulos.ordenes.domain.TipoDescuento;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RespuestaCupon {
    private UUID id;
    private String codigo;
    private TipoDescuento tipo;
    private BigDecimal valor;
    private LocalDateTime fechaExpiracion;
    private Integer limiteUsos;
    private int usosActuales;
    private boolean activo;
    private LocalDateTime creadoEn;
}
```

- [ ] **Step 2: Escribir el test que falla para `crearCupon`/`listarCupones`**

Agregar a `CasoUsoGestionarCuponTest.java`:

```java
@Test
void crearCuponDevuelveUnDtoNoLaEntidad() {
    when(repositorioCupon.findByIdTiendaAndCodigo(any(), anyString())).thenReturn(Optional.empty());
    when(repositorioCupon.save(any(Cupon.class))).thenAnswer(inv -> inv.getArgument(0));

    SolicitudCrearCupon solicitud = SolicitudCrearCupon.builder()
            .codigo("promo10")
            .tipo(TipoDescuento.PORCENTAJE)
            .valor(new BigDecimal("10"))
            .build();

    RespuestaCupon respuesta = casoUsoGestionarCupon.crearCupon(idTienda, solicitud);

    assertEquals("PROMO10", respuesta.getCodigo());
    assertEquals(TipoDescuento.PORCENTAJE, respuesta.getTipo());
    assertEquals(new BigDecimal("10"), respuesta.getValor());
}
```

(Import `RespuestaCupon` y `SolicitudCrearCupon` al inicio del archivo.)

- [ ] **Step 3: Correr el test y verificar que falla**

Run: `./gradlew test --tests "com.ecommerce.modulos.ordenes.application.CasoUsoGestionarCuponTest"`
Expected: FAIL — no compila (`crearCupon` todavía devuelve `Cupon`, no tiene `getCodigo()` con ese tipo de retorno esperado por el test... en realidad `Cupon` sí tiene `getCodigo()`, así que el fallo real es de tipo si el test asigna a `RespuestaCupon respuesta = ...crearCupon(...)` y el método devuelve `Cupon` — no compila, que es la señal esperada).

- [ ] **Step 4: Implementar el mapeo en `CasoUsoGestionarCupon`**

Agregar un método estático `mapToResponse(Cupon cupon)` (mismo patrón que `CasoUsoCrearProducto.mapToResponse`) y cambiar las firmas de `crearCupon`/`listarCupones`:

```java
@Transactional
public RespuestaCupon crearCupon(UUID idTienda, SolicitudCrearCupon request) {
    repositorioCupon.findByIdTiendaAndCodigo(idTienda, request.getCodigo().toUpperCase())
            .ifPresent(c -> {
                throw new ExcepcionRecursoDuplicado("Cupón", "código", request.getCodigo());
            });

    Cupon cupon = new Cupon();
    cupon.setIdTienda(idTienda);
    cupon.setCodigo(request.getCodigo().toUpperCase());
    cupon.setTipo(request.getTipo());
    cupon.setValor(request.getValor());
    cupon.setFechaExpiracion(request.getFechaExpiracion());
    cupon.setLimiteUsos(request.getLimiteUsos());

    return mapToResponse(repositorioCupon.save(cupon));
}

@Transactional(readOnly = true)
public RespuestaPaginada<RespuestaCupon> listarCupones(UUID idTienda, Pageable pageable) {
    Page<Cupon> page = repositorioCupon.findAllByIdTienda(idTienda, pageable);
    return RespuestaPaginada.from(page.map(CasoUsoGestionarCupon::mapToResponse));
}

private static RespuestaCupon mapToResponse(Cupon cupon) {
    return RespuestaCupon.builder()
            .id(cupon.getId())
            .codigo(cupon.getCodigo())
            .tipo(cupon.getTipo())
            .valor(cupon.getValor())
            .fechaExpiracion(cupon.getFechaExpiracion())
            .limiteUsos(cupon.getLimiteUsos())
            .usosActuales(cupon.getUsosActuales())
            .activo(cupon.isActivo())
            .creadoEn(cupon.getCreadoEn())
            .build();
}
```

Verificar que `Cupon`/`EntidadAuditableBase` expone `getCreadoEn()` (heredado) antes de compilar — si el getter se llama distinto, ajustar.

- [ ] **Step 5: Actualizar `ControladorCupon`**

Cambiar `ResponseEntity<Cupon>` → `ResponseEntity<RespuestaCupon>` en `crearCupon`, y `RespuestaPaginada<Cupon>` → `RespuestaPaginada<RespuestaCupon>` en `listarCupones`. Actualizar el import (quitar `Cupon` si ya no se usa en el archivo, agregar `RespuestaCupon`).

- [ ] **Step 6: Correr los tests y verificar que pasan**

Run: `./gradlew test --tests "com.ecommerce.modulos.ordenes.*"`
Expected: PASS, incluido `CuponConcurrenciaIntegrationTest` (no debería verse afectado — no toca `crearCupon`/`listarCupones`).

---

### Task 4: Validación de tamaño y de regla de negocio en DTOs de escritura

**Contexto del hallazgo:** Ninguno de estos campos tiene cota superior de tamaño hoy (verificado leyendo cada DTO completo): `SolicitudRegistro.contrasena`/`confirmarContrasena` (`@Size(min = 8)` sin `max`, `src/main/java/com/ecommerce/modulos/identidad/application/dto/SolicitudRegistro.java:22,25`), `SolicitudCrearProducto.nombre/enlaceCorto/descripcion/sku/codigoBarras` (sin `@Size`, `SolicitudCrearProducto.java`), `SolicitudCrearResena.titulo/comentario` (sin `@Size`, `SolicitudCrearResena.java`). Sin cota, un payload gigante llega hasta la capa de persistencia y falla como 500 (`DataIntegrityViolationException`/error de driver) en vez de 400. Además `SolicitudCrearCupon.valor` (`SolicitudCrearCupon.java:28-30`) solo valida `@Positive`: un cupón `tipo=PORCENTAJE, valor=500` es válido para Bean Validation y solo lo frena el `.min(subtotal)` de `CasoUsoGestionarCupon.calcularDescuento` en tiempo de uso — el dato inválido igual queda persistido.

**Files:**
- Modify: `src/main/java/com/ecommerce/modulos/identidad/application/dto/SolicitudRegistro.java`
- Modify: `src/main/java/com/ecommerce/modulos/catalogo/application/dto/SolicitudCrearProducto.java`
- Modify: `src/main/java/com/ecommerce/modulos/resenas/application/dto/SolicitudCrearResena.java`
- Modify: `src/main/java/com/ecommerce/modulos/ordenes/application/dto/SolicitudCrearCupon.java`
- Test: `src/test/java/com/ecommerce/modulos/identidad/application/dto/SolicitudRegistroValidationTest.java`, `.../catalogo/application/dto/SolicitudCrearProductoValidationTest.java`, `.../resenas/application/dto/SolicitudCrearResenaValidationTest.java`, `.../ordenes/application/dto/SolicitudCrearCuponValidationTest.java` (nuevos — ver Step 1; si el repo ya tiene un patrón de test de validación de DTOs con `Validator`/`ValidatorFactory`, usar ese patrón en vez de crear uno nuevo — buscar con `codegraph_search "ValidatorFactory"` antes de escribir).

**Interfaces:** No cambia ninguna firma pública, solo anotaciones de Bean Validation. `@Valid` ya está presente en los cuatro controllers que reciben estos DTOs (`ControladorAutenticacion.register`, `ControladorCatalogo.createProduct`, `ControladorResena.crearResena`, `ControladorCupon.crearCupon`) — confirmarlo antes de dar la tarea por completa, no volver a agregarlo si ya está.

**Valores exactos a aplicar (verbatim):**

| DTO.campo | Anotación a agregar |
|---|---|
| `SolicitudRegistro.contrasena` | `@Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")` (reemplaza el `@Size` actual) |
| `SolicitudRegistro.confirmarContrasena` | `@Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")` (reemplaza el `@Size` actual) |
| `SolicitudCrearProducto.nombre` | `@Size(max = 200, message = "El nombre no puede superar los 200 caracteres")` (además del `@NotBlank` ya existente) |
| `SolicitudCrearProducto.enlaceCorto` | `@Size(max = 200, message = "El enlace corto no puede superar los 200 caracteres")` (además del `@NotBlank` ya existente) |
| `SolicitudCrearProducto.descripcion` | `@Size(max = 5000, message = "La descripción no puede superar los 5000 caracteres")` |
| `SolicitudCrearProducto.sku` | `@Size(max = 100, message = "El SKU no puede superar los 100 caracteres")` |
| `SolicitudCrearProducto.codigoBarras` | `@Size(max = 100, message = "El código de barras no puede superar los 100 caracteres")` |
| `SolicitudCrearResena.titulo` | `@Size(max = 200, message = "El título no puede superar los 200 caracteres")` (además del `@NotBlank` ya existente) |
| `SolicitudCrearResena.comentario` | `@Size(max = 2000, message = "El comentario no puede superar los 2000 caracteres")` (además del `@NotBlank` ya existente) |

Los imports `jakarta.validation.constraints.Size` ya están presentes en `SolicitudRegistro.java` y `SolicitudCrearResena.java`; agregarlo en `SolicitudCrearProducto.java`.

Para `SolicitudCrearCupon.valor` (regla condicional, no es un simple `@Size`/`@Max`): agregar un método `@AssertTrue` a la clase:

```java
@AssertTrue(message = "El valor de un descuento porcentual no puede superar 100")
public boolean isValorPorcentualValido() {
    if (tipo != TipoDescuento.PORCENTAJE || valor == null) {
        return true;
    }
    return valor.compareTo(new BigDecimal("100")) <= 0;
}
```

(Import `jakarta.validation.constraints.AssertTrue`. Con Lombok `@Data`, este método con prefijo `is` sobre un `boolean` no colisiona con ningún getter generado porque no hay campo `valorPorcentualValido` — confirmar que compila; si Lombok se queja, renombrar el método a algo que no empiece con `is`/`get` seguido del nombre de un campo real y usar `@AssertTrue` igual, Bean Validation no depende del prefijo del nombre del método más que para el mensaje por defecto.)

- [ ] **Step 1: Escribir los tests de validación que fallan**

Para cada DTO, un test mínimo tipo:

```java
package com.ecommerce.modulos.ordenes.application.dto;

import com.ecommerce.modulos.ordenes.domain.TipoDescuento;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SolicitudCrearCuponValidationTest {

    static ValidatorFactory factory;
    static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    void rechazaPorcentajeMayorA100() {
        SolicitudCrearCupon solicitud = SolicitudCrearCupon.builder()
                .codigo("PROMO")
                .tipo(TipoDescuento.PORCENTAJE)
                .valor(new BigDecimal("500"))
                .build();

        Set<ConstraintViolation<SolicitudCrearCupon>> violations = validator.validate(solicitud);

        assertFalse(violations.isEmpty());
    }

    @Test
    void aceptaPorcentajeDentroDeRango() {
        SolicitudCrearCupon solicitud = SolicitudCrearCupon.builder()
                .codigo("PROMO")
                .tipo(TipoDescuento.PORCENTAJE)
                .valor(new BigDecimal("50"))
                .build();

        assertTrue(validator.validate(solicitud).isEmpty());
    }

    @Test
    void aceptaMontoFijoMayorA100() {
        SolicitudCrearCupon solicitud = SolicitudCrearCupon.builder()
                .codigo("PROMO")
                .tipo(TipoDescuento.MONTO_FIJO)
                .valor(new BigDecimal("500"))
                .build();

        assertTrue(validator.validate(solicitud).isEmpty());
    }
}
```

Replicar el mismo esqueleto (`ValidatorFactory`/`Validator`) para los otros tres DTOs, con un test por campo que verifique que un valor que excede el `max` produce una violación y uno dentro del límite no la produce.

- [ ] **Step 2: Correr los tests y verificar que fallan**

Run: `./gradlew test --tests "*ValidationTest"`
Expected: FAIL en los casos que esperan violación (los DTOs todavía no tienen las anotaciones).

- [ ] **Step 3: Aplicar las anotaciones de la tabla y el `@AssertTrue` de `SolicitudCrearCupon`**

- [ ] **Step 4: Correr los tests y verificar que pasan**

Run: `./gradlew test --tests "*ValidationTest"`
Expected: PASS.

- [ ] **Step 5: Correr la suite completa para descartar regresiones**

Run: `./gradlew test`
Expected: todos los tests en verde (o solo los `*IntegrationTest` fallando por falta de Docker, documentado como tal).

---

## Verificación final de la Fase 9

1. `./gradlew build` en verde (o con los fallos de Testcontainers documentados si no hay Docker en este entorno).
2. Manualmente (si hay entorno local con `docker-compose -f docker/docker-compose.yml up -d` y `./gradlew bootRun`): confirmar en Swagger (`http://localhost:8081/swagger-ui.html`) que `GET /api/v1/catalogo/productos` para una tienda con >20 productos no degrada de forma perceptible, que `GET /api/v1/busqueda?categoria=...&minPrice=...&page=1` realmente filtra/pagina, y que `POST /api/v1/cupones` devuelve un JSON sin campos de auditoría/tenant internos.
3. PR con título `refactor(api): eliminar N+1 de catalogo, busqueda real y DTOs de cupon/validacion` (o el que el título Conventional Commit final determine), squash-merge, esperar CI en verde antes de mergear — mismo flujo que las Fases 0-8.
