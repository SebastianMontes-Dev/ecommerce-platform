---
name: desarrollo-backend-spring
description: >-
  Usar esta skill al diseñar, implementar, refactorizar o testear funcionalidades de backend en Java 21 y Spring Boot 3.4.4 dentro de ecommerce-platform, asegurando la arquitectura en 3 capas, aislamiento multi-tenant, DDD ligero y cobertura con JUnit 5/Testcontainers.
---

# Skill: Desarrollo Backend en Spring Boot (NexaSaaS)

Guía paso a paso y mejores prácticas para el desarrollo de módulos backend en **Java 21** y **Spring Boot 3.4.4 (Gradle)** en el monolito modular **NexaSaaS**.

---

## 1. Localización y Estructura del Módulo

Todo desarrollo de funcionalidad de negocio debe residir en su módulo correspondiente bajo:
`src/main/java/com/ecommerce/modulos/<modulo>/`

Cada módulo se organiza en 3 capas internas (DDD ligero):
```
modulos/<modulo>/
├── domain/                  # Entidades JPA, eventos de dominio, value objects, excepciones de dominio
│   └── events/              # Eventos que disparan acciones asíncronas
├── application/             # Casos de uso / servicios de aplicación, DTOs y Java records
│   ├── dto/                 # RequestDTOs y ResponseDTOs (inmutables)
│   └── usecase/             # CasoUsoCrearX, CasoUsoObtenerX (o ServicioX)
└── infrastructure/          # Adaptadores externos: controladores REST, repositorios JPA, pasarelas
    ├── controller/          # ControladorREST (endpoints @RestController)
    └── persistence/         # RepositorioJPA (Spring Data JPA)
```

Componentes transversales residen en `com.ecommerce.compartido` (`RedisConfig`, `SecurityConfig`, `ConfiguracionMultiTenantDB`, etc.).

---

## 2. Flujo de Implementación Paso a Paso

### Paso 1: Capa de Dominio (`domain/`)
1. **Entidades**: Definir la entidad JPA con `@Entity`, `@Table(name = "...")` y validaciones Bean Validation (`@NotNull`, `@NotBlank`, `@Size`).
2. **Aislamiento Multi-Tenant**:
   - Todo dato perteneciente a un inquilino debe asociarse con `inquilinoId`.
   - Si aplica filtro a nivel de fila, utilizar `@Filter(name = "filtroInquilino")` (`ConfiguracionFiltroInquilinoHibernate`).
3. **Eventos**: Si una operación dispara acciones secundarias (envío de email, recálculo de métricas), define un evento inmutable (Java `record`).

### Paso 2: Esquema de Base de Datos (Flyway)
1. **Nunca depender de `ddl-auto`** para la producción o tests.
2. Si se crean o alteran tablas, crear un nuevo script Flyway en:
   `src/main/resources/db/migration/V{siguiente}__{descripcion_en_espanol}.sql`
   (Ej. `V9__crear_tabla_recompensas.sql`).
3. Validar sintaxis PostgreSQL (tipos `UUID`, `TIMESTAMP WITH TIME ZONE`, `BIGINT`, `NUMERIC(19,4)` para dinero).

### Paso 3: Capa de Aplicación (`application/`)
1. **DTOs y Records**: Crear Java `record` para los contratos de entrada y salida:
   - `CrearProductoRequest(String nombre, BigDecimal precio, ...)`
   - `ProductoResponse(UUID id, String nombre, BigDecimal precio, ...)`
2. **Casos de Uso / Servicios**:
   - Marcar métodos con `@Transactional` (o `@Transactional(readOnly = true)` en consultas).
   - Validar reglas de negocio y restricciones de inquilino.
   - **REGLA ESTRICTA**: **Nunca retornar entidades JPA en la firma del método**. Retornar siempre `record` o DTO mapeado.

### Paso 4: Capa de Infraestructura (`infrastructure/`)
1. **Repositorio**: Crear interface `RepositorioX` extendiendo `JpaRepository<X, UUID>`.
2. **Controlador REST**:
   - `@RestController`, `@RequestMapping("/api/v1/<modulo>")`.
   - Inyectar el caso de uso/servicio (nunca el repositorio directamente).
   - Validar el body con `@Valid`.
   - Retornar `ResponseEntity<T>` con los códigos HTTP correspondientes (`200 OK`, `201 Created`, `204 No Content`, `404 Not Found`).
   - Sin lógica de negocio ni cálculos en el controlador.

### Paso 5: Asincronía y Eventos
- **Recordatorio Crítico**: RabbitMQ no está conectado en el código Java.
- Los eventos asíncronos se manejan in-process con:
  - Publicación: `ApplicationEventPublisher.publishEvent(new EventoX(...))`
  - Consumo: `@Async` y `@EventListener` en la clase oyente (ej. `OyenteEventoOrden`).

---

## 3. Estrategia de Pruebas

Toda nueva funcionalidad debe incluir cobertura de tests:

1. **Test Unitario (`*Test`)**:
   - Usar JUnit 5 (`@ExtendWith(MockitoExtension.class)`).
   - Mockear dependencias del caso de uso con `@Mock` y `@InjectMocks`.
   - Probar casos de éxito y casos de error (excepciones de negocio, validaciones).
2. **Test de Integración (`*IntegrationTest`)**:
   - Usar `@SpringBootTest` con Testcontainers para PostgreSQL o Elasticsearch.
   - Recordar que en tests locales manuales, `application-test.yml` apunta al puerto **5432**.
3. **Test de Controlador**:
   - Usar `MockMvc` o Rest Assured para verificar los contratos HTTP y códigos de estado.

---

## 4. Comandos de Verificación

Antes de dar por terminada la tarea, ejecutar:

```bash
# Ejecutar suite de tests completa
./gradlew test

# Compilación y empaquetado
./gradlew build -x test  # o build completo
```
