# ecommerce-platform (NexaSaaS) — Guía y Reglas para Agentes

API REST de e-commerce multi-tenant (SaaS B2B/B2C) sobre **Java 21 + Spring Boot 3.4.4 (Gradle)**. Monolito modular con aislamiento de datos por inquilino:
- **Tenants estándar**: Row-level filtering con Hibernate `@Filter` (`ConfiguracionFiltroInquilinoHibernate`).
- **Tenants premium**: Base de datos dedicada con enrutamiento dinámico vía `AbstractRoutingDataSource` (`ConfiguracionMultiTenantDB`).

Este documento define el contexto técnico, comandos operativos, arquitectura y reglas obligatorias para cualquier agente que opere en este repositorio.

---

## Comandos Verificados

```bash
# Compilación y ejecución de tests
./gradlew build

# Levantar la aplicación en :8081 (perfil "dev" activo por defecto)
./gradlew bootRun

# Ejecución exclusiva de tests (JUnit 5 vía useJUnitPlatform)
./gradlew test

# Levantar infraestructura local completa
docker-compose -f docker/docker-compose.yml up -d
# Contenedores: Postgres, Redis, RabbitMQ, MinIO, Elasticsearch, Mailhog, Prometheus, Grafana, Zipkin
```

### Puertos y Servicios Críticos

> [!WARNING]
> **Puerto Postgres no estándar: `5433` (no `5432`) en desarrollo:**
> - `docker/docker-compose.yml` mapea `5433:5432` en el host.
> - `application.yml` conecta a `jdbc:postgresql://localhost:5433/ecommerce_db`.
> - **Atención en tests:** `src/test/resources/application-test.yml` apunta a `localhost:5432/ecommerce_test`. Si ejecutas tests de integración contra un Postgres levantado manualmente o vía docker-compose, debes asegurar que el puerto 5432 esté expuesto o ajustar la configuración de pruebas según aplique (Testcontainers maneja sus propios puertos efímeros).

- **Documentación Swagger UI**: `http://localhost:8081/swagger-ui.html`
- **Spring Actuator**: expone endpoints en `/actuator` (`health`, `info`, `metrics`, `prometheus`).
- **Variables de Entorno**: Consultar `.env.example` (Postgres, Redis, RabbitMQ, JWT, MinIO, Stripe, Elasticsearch). Copiar a `.env` local. **Nunca commitear secretos ni tokens.**

---

## Convenciones de Commits, Ramas y PRs

Alineadas estrictamente con [`CONTRIBUTING.md`](CONTRIBUTING.md):

1. **Commits (Conventional Commits en español)**:
   - Formato: `tipo(scope): descripción en español, imperativo, sin punto final`
   - **Tipos** (en inglés): `feat`, `fix`, `docs`, `refactor`, `test`, `chore`, `perf`, `style`, `build`, `ci`, `revert`.
   - **Scope** (opcional): Nombre del módulo/carpeta en singular/plural idéntico al código (`identidad`, `inquilino`, `catalogo`, `busqueda`, `carrito`, `ordenes`, `pagos`, `notificacion`, `logistica`, `resenas`, `analiticas`, `ia`, `compartido`).
   - Ejemplos:
     - `feat(pagos): agregar pasarela de MercadoPago`
     - `fix(ordenes): validar que el cliente autenticado sea dueño de la orden`
     - `docs: actualizar ROADMAP con la fase de internacionalización`
2. **Ramas**:
   - Formato: `tipo/slug-corto-en-espanol-kebab-case` (ej. `feat/checkout-multipasarela`, `fix/aislamiento-multi-tenant`).
3. **Pull Requests**:
   - Título idéntico a Conventional Commits.
   - Plantilla obligatoria: [`.github/pull_request_template.md`](.github/pull_request_template.md) (Resumen, Cambios, Plan de pruebas).
   - **Squash-merge por defecto**; **CI en verde obligatorio** antes de mergear.

> [!CRITICAL]
> **Prohibición absoluta de atribución de IA en Git:**
> Nunca agregar `Co-Authored-By: Claude`, `Co-Authored-By: Gemini`, `Co-Authored-By: Antigravity` ni ninguna variante de trailer de IA a commits o PRs. Todos los commits deben registrarse únicamente bajo la cuenta y autoría del usuario.

---

## Estructura Modular y Arquitectura

Todo el código de producción reside bajo `src/main/java/com/ecommerce/`:

```
com.ecommerce/
├── bootstrap/                    # AplicacionEcommerce (único punto de entrada @SpringBootApplication)
├── modulos/                      # Monolito modular (DDD ligero en 3 capas internas: domain / application / infrastructure)
│   ├── identidad/                # Usuarios, autenticación, roles, Spring Security + JWT
│   ├── inquilino/                # Tenants, suscripciones, enrutamiento dinámico de DB
│   ├── catalogo/                 # Productos, categorías, variantes (Postgres)
│   ├── busqueda/                 # Indexación y consultas con Elasticsearch (CQRS de lectura)
│   ├── carrito/                  # Carrito de compras en memoria/sesión (Redis)
│   ├── ordenes/                  # Gestión de pedidos y eventos de dominio (domain/events)
│   ├── pagos/                    # Integración con Stripe; patrón Strategy en infrastructure/pasarelas
│   ├── notificacion/             # Consumo de eventos de orden + emails con plantillas Thymeleaf
│   ├── logistica/                # Envíos y cálculo de tarifas
│   ├── resenas/                  # Calificaciones y comentarios de productos
│   ├── analiticas/               # Métricas de ventas y reportes
│   └── ia/                       # Asistente virtual y recomendaciones
└── compartido/                   # Componentes cross-cutting:
    ├── ConfiguracionMultiTenantDB.java
    ├── ConfiguracionFiltroInquilinoHibernate.java
    ├── RedisConfig.java
    ├── SecurityConfig.java
    ├── RateLimitConfig.java      # Bucket4j (InterceptorLimiteTasa)
    ├── ConfiguracionObservabilidad.java # Zipkin, Micrometer
    └── WebSocketConfig.java
```

### Reglas de Diseño y Nomenclatura
- **Capas estrictas**:
  - `ControladorX`: Validación HTTP y delegación. Sin lógica de negocio.
  - `CasoUsoX` / `ServicioX`: Reglas de negocio y orquestación. **Nunca retornar entidades JPA a la API**, siempre retornar DTOs o Java `record`.
  - `RepositorioX`: Persistencia Spring Data JPA.
- **Idioma**: Nombres de clases, interfaces, métodos y variables de negocio en **español** (`ServicioCarrito`, `CasoUsoCrearOrden`, `ControladorCatalogo`).
- **Base de Datos y Flyway**: Migraciones SQL en `src/main/resources/db/migration/V1..V8`. Aunque dev tenga `ddl-auto: update`, Flyway es la única fuente de verdad para el esquema.

---

## Estado Real de Tecnologías (Verificado en Código)

No asumir funcionalidades por la presencia de dependencias; el estado real es:

1. **Redis: IMPLEMENTADO Y ACTIVO.**
   - `RedisConfig` en `compartido/infrastructure` define `RedisTemplate` (serialización JSON con Jackson) y `RedisCacheManager` (`@EnableCaching`, TTL de 10 minutos).
   - Se utiliza activamente en `ServicioCarrito` (persistencia del carrito), `ControladorCatalogo` y `InterceptorLimiteTasa` (rate limiting con Bucket4j).
2. **AMQP / RabbitMQ: NO IMPLEMENTADO EN CÓDIGO (Solo Infra/Dependencia).**
   - Aunque `spring-boot-starter-amqp` está en `build.gradle` y RabbitMQ corre en `docker-compose`, **no hay ningún `@RabbitListener`, `RabbitTemplate`, `Queue` ni `Exchange`** en el código Java.
   - La comunicación asíncrona actual se realiza enteramente in-process mediante Spring events con `@EventListener` y `@Async` (ej. `OyenteEventoOrden`). **No asumas colas RabbitMQ activas en lógica nueva sin antes implementarlas explícitamente.**
3. **GraphQL: IMPLEMENTADO PERO MÍNIMO.**
   - Configurado con `spring-boot-starter-graphql` y esquema en `src/main/resources/graphql/schema.graphqls`.
   - Único endpoint activo: `ControladorGraphQLProducto` (query `obtenerProductoPorId`), que es una capa fina sobre `CasoUsoObtenerProducto`. No existe cobertura GraphQL para el resto de módulos.

---

## CodeGraph y Servidores MCP

- **CodeGraph**: El repositorio cuenta con índice preconstruido en `.codegraph/`. Para consultas de impacto (`codegraph_impact`), referencias (`codegraph_callers`) o definición de símbolos (`codegraph_search`), utiliza CodeGraph para respuesta inmediata con AST de tree-sitter. Para búsqueda textual literal de logs o comentarios, utiliza `grep_search`.
- **Servidores MCP locales (`.mcp.json`)**:
  - `postgres`: `@henkey/postgres-mcp-server` (conecta al puerto 5433 vía `POSTGRES_CONNECTION_STRING`).
  - `github`: Servidor GitHub en Docker para PRs/Issues (vía `GITHUB_PERSONAL_ACCESS_TOKEN`).

---

## Estrategia de Testing

- **Ubicación y simetría**: `src/test/java` replica la estructura de paquetes de `src/main/java`.
- **Tests Unitarios**: Nombres `*Test` con JUnit 5 y Mockito.
- **Tests de Integración**: Nombres `*IntegrationTest` utilizando Testcontainers para levantar Postgres y Elasticsearch en entornos limpios y reproducibles.
- **Configuración de prueba (`application-test.yml`)**:
  - Base de datos `ecommerce_test` en puerto `5432`.
  - `ddl-auto: validate` (requiere que las migraciones Flyway estén al día).
  - Listener de RabbitMQ desactivado (`auto-startup: false`).
- **Tests de Controladores**: Uso de Rest Assured / `MockMvc` para validar contratos REST y códigos de respuesta HTTP.

---

## Documentación Continua Obligatoria de Cambios

> [!IMPORTANT]
> **Regla de oro: Trazabilidad absoluta para nunca perder el hilo:**
> 1. **Documentar cada cambio o implementación**: Ningún cambio de código, fix, refactor o nueva característica debe darse por terminado sin documentar qué se hizo, por qué y en qué archivos.
> 2. **Actualización de documentación viva**:
>    - Planes de trabajo y avances: Registrar en `docs/superpowers/plans/` (o crear la bitácora correspondiente si es una nueva tarea).
>    - Decisiones arquitectónicas: Crear o actualizar ADRs en `docs/architecture/` (`ADR-00X-...`).
>    - Hitos de producto: Actualizar los checkboxes de avance en `ROADMAP.md`.
> 3. **Resumen de contexto en traspasos**: Al finalizar cada turno o tarea, proporcionar una síntesis clara con:
>    - Cambios realizados y rationale técnico.
>    - Validaciones ejecutadas (build, tests, docker).
>    - Estado actual del sistema y próximos pasos inmediatos.

---

## Skills de Soporte y Desarrollo Backend

El espacio de trabajo cuenta con una suite completa de **7 Skills modulares** en `.agents/skills/` diseñadas para apoyar la memoria, persistencia de contexto y desarrollo backend con Spring Boot:

1. **`bitacora-y-contexto` (`.agents/skills/bitacora-y-contexto/SKILL.md`)**:
   - Registro continuo de avances, planes de tareas y hand-offs para nunca perder el hilo técnico.
2. **`desarrollo-backend-spring` (`.agents/skills/desarrollo-backend-spring/SKILL.md`)**:
   - Runbook paso a paso de arquitectura en 3 capas, DDD ligero, `@Transactional`, DTOs/records y aislamiento multi-tenant.
3. **`testing-y-testcontainers` (`.agents/skills/testing-y-testcontainers/SKILL.md`)**:
   - Tests unitarios con Mockito, tests de integración con Testcontainers (Postgres y Elasticsearch efímeros) y tests de controladores REST.
4. **`auditoria-seguridad-y-calidad` (`.agents/skills/auditoria-seguridad-y-calidad/SKILL.md`)**:
   - Prevención de fugas de secretos (gitleaks), Spring Security + JWT, sanitización de entradas y análisis con SpotBugs o Trivy.
5. **`migraciones-flyway-y-db` (`.agents/skills/migraciones-flyway-y-db/SKILL.md`)**:
   - Convenciones de Flyway (`V...__*.sql`), tipos PostgreSQL, columnas `inquilino_id`, claves foráneas e índices.
6. **`infraestructura-y-docker` (`.agents/skills/infraestructura-y-docker/SKILL.md`)**:
   - Operación de contenedores (Postgres en puerto 5433, Redis, Elasticsearch, RabbitMQ, Mailhog) y observabilidad (Actuator, Zipkin, Prometheus).
7. **`flujo-git-y-entregas` (`.agents/skills/flujo-git-y-entregas/SKILL.md`)**:
   - Ramas en español, Conventional Commits en español con tipos en inglés, plantilla de PR y garantía absoluta de cero trailers de autoría a IA.

