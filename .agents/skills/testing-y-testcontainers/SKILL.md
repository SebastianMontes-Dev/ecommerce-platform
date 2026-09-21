---
name: testing-y-testcontainers
description: >-
  Usar esta skill al diseñar, escribir, ejecutar o refactorizar pruebas unitarias (JUnit 5, Mockito) y de integración con Testcontainers (Postgres, Elasticsearch) o pruebas de controladores REST con MockMvc en ecommerce-platform.
---

# Skill: Testing y Testcontainers (NexaSaaS)

Esta skill define la estrategia y ejecución de pruebas para asegurar la calidad y estabilidad de la API sin comprometer entornos de producción ni depender de bases de datos compartidas.

---

## 1. Pirámide de Pruebas en el Repositorio

1. **Pruebas Unitarias (`*Test.java`)**:
   - **Alcance**: Lógica pura de casos de uso, validaciones de dominio, mapeadores y componentes aislados.
   - **Velocidad**: Sub-segundo, en memoria, sin levantar contexto de Spring.
   - **Herramientas**: JUnit 5 (`@ExtendWith(MockitoExtension.class)`), Mockito (`@Mock`, `@InjectMocks`, `when()`, `verify()`).
2. **Pruebas de Integración (`*IntegrationTest.java`)**:
   - **Alcance**: Persistencia JPA real, consultas con Hibernate `@Filter`, migraciones Flyway y comunicación con Elasticsearch.
   - **Herramientas**: `@SpringBootTest`, **Testcontainers** para Postgres y Elasticsearch (puertos efímeros aislados).
3. **Pruebas de Controladores REST**:
   - **Herramientas**: `MockMvc` o Rest Assured (`spring-mock-mvc`) para validar contratos HTTP, serialización JSON y códigos de estado.

---

## 2. Plantilla: Test Unitario de Caso de Uso

Ubicación: `src/test/java/com/ecommerce/modulos/<modulo>/application/`

```java
@ExtendWith(MockitoExtension.class)
class CasoUsoCrearProductoTest {

    @Mock
    private RepositorioProducto repositorioProducto;

    @InjectMocks
    private CasoUsoCrearProducto casoUso;

    @Test
    @DisplayName("Debe crear producto exitosamente cuando los datos son válidos")
    void debeCrearProductoExitosamente() {
        // Given
        var request = new CrearProductoRequest("Laptop Pro", new BigDecimal("1500.00"), "SKU-123");
        var productoGuardado = new Producto(UUID.randomUUID(), request.nombre(), request.precio());
        when(repositorioProducto.save(any(Producto.class))).thenReturn(productoGuardado);

        // When
        var resultado = casoUso.ejecutar(request);

        // Then
        assertNotNull(resultado);
        assertEquals("Laptop Pro", resultado.nombre());
        verify(repositorioProducto, times(1)).save(any(Producto.class));
    }
}
```

---

## 3. Plantilla: Test de Integración con Testcontainers

Ubicación: `src/test/java/com/ecommerce/modulos/<modulo>/infrastructure/`

```java
@SpringBootTest
@Testcontainers
class RepositorioProductoIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("ecommerce_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private RepositorioProducto repositorio;

    @Test
    @DisplayName("Debe filtrar productos por inquilino activo en la sesión")
    void debeFiltrarPorInquilino() {
        // Validación contra PostgreSQL real con migraciones Flyway aplicadas
    }
}
```

---

## 4. Diferencia Crítica de Puertos en Pruebas Locales

> [!WARNING]
> Si ejecutas pruebas sin Testcontainers contra infraestructura local levantada con Docker Compose:
> - `application.yml` (dev) usa el puerto **5433**.
> - `src/test/resources/application-test.yml` espera Postgres en el puerto **5432**.
> - `ddl-auto: validate`: Las migraciones Flyway **deben** estar aplicadas previamente en la base de datos de prueba `ecommerce_test`.

---

## 5. Comandos de Ejecución

```bash
# Ejecutar todas las pruebas del proyecto
./gradlew test

# Ejecutar una clase de test específica
./gradlew test --tests com.ecommerce.modulos.catalogo.application.CasoUsoCrearProductoTest

# Ejecutar únicamente tests de integración
./gradlew test --tests *IntegrationTest

# Ver reporte HTML generado tras los tests
# build/reports/tests/test/index.html
```
