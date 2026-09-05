# ecommerce-platform (NexaSaaS)

API REST de e-commerce multi-tenant (SaaS B2B/B2C) sobre Java 21 + Spring Boot 3.4.4 (Gradle). Monolito modular con aislamiento de datos por inquilino (row-level filtering con Hibernate `@Filter` para tenants estándar, `AbstractRoutingDataSource` para tenants premium con DB dedicada).

## Comandos verificados

```bash
./gradlew build          # compila + corre tests
./gradlew bootRun        # levanta la app en :8081 (perfil "dev" por defecto)
./gradlew test           # solo tests (JUnit 5 vía useJUnitPlatform)
docker-compose -f docker/docker-compose.yml up -d   # infra: postgres, redis, rabbitmq, minio, elasticsearch, mailhog, prometheus, grafana, zipkin
```

**Puerto Postgres no estándar: `5433`, no `5432`.** `docker/docker-compose.yml` mapea `5433:5432` y `application.yml` apunta a `jdbc:postgresql://localhost:5433/ecommerce_db`. Ojo: `src/test/resources/application-test.yml` usa `localhost:5432/ecommerce_test` (puerto default, distinto al de dev) — si corrés Postgres solo vía el docker-compose del repo, los tests de integración que dependan de esa DB fallarán a menos que expongas también el 5432, o ajustes el yml de test.

Swagger UI: `http://localhost:8081/swagger-ui.html`. Actuator expone `health,info,metrics,prometheus`.

Variables de entorno relevantes están en `.env.example` (DB, Redis, RabbitMQ, JWT, MinIO, Stripe, Elasticsearch) — copiar a `.env`, nunca commitear secretos.

## Convenciones de commits, ramas y PRs

Ver [`CONTRIBUTING.md`](CONTRIBUTING.md) — Conventional Commits en español (`tipo(scope): descripción`), ramas `tipo/slug-en-espanol`, PRs con el mismo formato de título y plantilla en [`.github/pull_request_template.md`](.github/pull_request_template.md). Squash-merge por defecto, CI en verde antes de mergear, nunca `Co-Authored-By: Claude`.

### MCP servers
`.mcp.json` ya wirea un servidor Postgres (`@henkey/postgres-mcp-server`, lee `POSTGRES_CONNECTION_STRING`, puerto 5433) y uno de GitHub (docker-based, `GITHUB_PERSONAL_ACCESS_TOKEN`). Ambos quedan inactivos hasta definir esas env vars localmente.

## Estructura de módulos

Todo el código vive bajo `src/main/java/com/ecommerce/`:
- `bootstrap/` — `AplicacionEcommerce` (main class), único punto de entrada.
- `modulos/<dominio>/` — cada dominio de negocio en capas `domain/ · application/ · infrastructure/` (DDD ligero, sin dependencias entre capas hacia infra):
  - `identidad/` — usuarios, auth, JWT.
  - `inquilino/` — tenants, planes, enrutamiento de DB (multi-tenant).
  - `catalogo/` — productos, variantes (Postgres).
  - `busqueda/` — indexación/consulta en Elasticsearch (CQRS de lectura sobre catálogo).
  - `carrito/` — carrito de compra, backed por Redis.
  - `ordenes/` — órdenes, eventos de dominio (`domain/events`).
  - `pagos/` — Stripe, patrón Strategy en `infrastructure/pasarelas`.
  - `notificacion/` — listener de eventos de orden + envío de correo (Thymeleaf).
  - `logistica/`, `resenas/`, `analiticas/`, `ia/` — logística, reseñas, reportes/analítica, asistente IA.
  - `compartido/` — cross-cutting: `RedisConfig`, `SecurityConfig`, `ConfiguracionMultiTenantDB`, `ConfiguracionFiltroInquilinoHibernate`, `RateLimitConfig` (Bucket4j), `ConfiguracionObservabilidad` (Zipkin/Micrometer), WebSocket config.

Convención de nombres en español (`ServicioX`, `CasoUsoX`, `ControladorX`, `RepositorioX`) — seguirla al agregar código nuevo.

Migraciones Flyway en `src/main/resources/db/migration/V1..V8`, `ddl-auto: update` en dev pero Flyway es la fuente real de verdad del esquema.

## Redis, AMQP y GraphQL — estado real (verificado en código, no en docs)

- **Redis: implementado.** `RedisConfig` (`compartido/infrastructure`) define `RedisTemplate` (serialización JSON con Jackson) y un `RedisCacheManager` (`@EnableCaching`, TTL 10 min). Se usa en `ServicioCarrito` (carrito activo), `ControladorCatalogo` y `InterceptorLimiteTasa` (rate limiting con Bucket4j).
- **AMQP/RabbitMQ: NO implementado, solo dependencia + infra.** `spring-boot-starter-amqp` está en `build.gradle`, RabbitMQ corre en `docker-compose.yml` y `application.yml` tiene credenciales de conexión, pero no hay ningún `@RabbitListener`, `RabbitTemplate`, `Queue`/`Exchange`/`Binding` en `src/main/java`. Los eventos de dominio (ej. cambio de estado de orden → notificación) se manejan con `@EventListener` + `@Async` in-process (`OyenteEventoOrden`), no con mensajería real. Si vas a tocar notificaciones o eventos asíncronos, es Spring events, no colas.
- **GraphQL: implementado, pero mínimo.** `spring-boot-starter-graphql` + schema en `src/main/resources/graphql/schema.graphqls` + un único controller (`ControladorGraphQLProducto`, query `obtenerProductoPorId`). Es una capa fina sobre el mismo caso de uso que ya expone REST (`CasoUsoObtenerProducto`); no asumas cobertura GraphQL de otros dominios sin verificar.

## Testing

- `src/test/java` sigue la misma estructura de paquetes que `main` (`modulos/<dominio>/application|infrastructure`), con tests unitarios (`*Test`) e de integración (`*IntegrationTest`, usan Testcontainers — hay dependencias para Postgres y Elasticsearch).
- `src/test/resources/application-test.yml`: DB `ecommerce_test` en puerto **5432** (no 5433), `ddl-auto: validate` (no crea/actualiza esquema, exige que Flyway ya lo haya migrado), listener de RabbitMQ desactivado (`auto-startup: false`).
- Rest Assured (`spring-mock-mvc`) disponible para tests de controllers.
