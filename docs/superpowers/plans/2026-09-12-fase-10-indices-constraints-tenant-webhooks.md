# Fase 10 — Base de datos: índices, constraints y tuning (nivel senior/DBA)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) o superpowers:executing-plans para implementar este plan tarea por tarea. Los pasos usan sintaxis de checkbox (`- [ ]`) para seguimiento.

**Goal:** Cerrar la Fase 10 de la ETAPA 2 (auditoría de continuación de NexaSaaS): el esquema de Postgres tiene índices faltantes sobre columnas que sí se consultan (confirmado leyendo cada `RepositorioX` real), índices duplicados sobre columnas que ya tienen un índice único implícito, ninguna columna monetaria/de inventario tiene un `CHECK` que la defienda de un `UPDATE` directo que se salte la capa de dominio, la columna `version` de optimistic locking es `NULLABLE` en las 21 tablas que la tienen (funciona hoy solo porque Hibernate siempre la puebla), `webhook_tenants` es la única tabla/entidad del proyecto que no participa del aislamiento por tenant (`id_tienda` en vez de `tenant_id`, sin `@Filter`, `RepositorioWebhookTenant` no extiende `RepositorioJpaBase`), no hay tuning de pool de conexiones en producción, y el índice de Elasticsearch depende enteramente de dynamic mapping (riesgo de que `idTienda` se infiera con un tipo distinto a `keyword` si el primer documento indexado no tiene el shape esperado).

**Architecture:** Sin cambios de arquitectura. Es una migración Flyway aditiva (`V16`) más ajustes puntuales de entidades/config ya existentes. `webhook_tenants` pasa a seguir exactamente el mismo patrón que ya usan `Cupon`/`Categoria` (`extends EntidadInquilino`, `RepositorioX extends RepositorioJpaBase<X>`).

**Tech Stack:** Java 21, Spring Boot 3.4.4, Flyway, PostgreSQL 15/16, HikariCP, `co.elastic.clients:elasticsearch-java:8.18.0`.

**Spec:** Este plan documenta su propio spec — surge de la ETAPA 2 de la auditoría de continuación de NexaSaaS (sesión 2026-09-11/12), Fase 10 del roadmap ahí definido. Cada afirmación de este plan se verificó leyendo las migraciones (`V1`..`V15`) y los repositorios/entidades reales, no la documentación.

## Global Constraints

- La migración es **aditiva**: no se borra ni se renombra ninguna tabla en este plan (`refunds`/`direcciones` se eliminan en la Fase 11, un plan separado — no tocarlas acá salvo el `NOT NULL` de `version`, que es seguro incluso si se van a borrar después).
- `ddl-auto: validate` en dev/test: cualquier cambio de columna/tabla que toque una entidad JPA existente tiene que reflejarse en la migración exactamente, o `./gradlew bootRun`/los tests de integración van a fallar al arrancar el contexto de Spring. Antes de dar una tarea por completa, correr `./gradlew build` (no solo `test`) para que Hibernate valide el esquema resultante contra las entidades.
- Convención de nombres en español del proyecto (`ServicioX`, `CasoUsoX`, `ControladorX`, `RepositorioX`) para el código nuevo; los nombres de columnas SQL siguen el estilo ya usado en las migraciones existentes (snake_case, mezcla de columnas en inglés/español heredada de fases anteriores — no es objetivo de este plan unificar eso).
- No introducir dependencias nuevas.
- Commits en español, Conventional Commits, uno por tarea — sin `Co-Authored-By` de ningún asistente de IA.
- Los `*IntegrationTest` con Testcontainers pueden no correr en este entorno (sin Docker) — si es así, documentarlo, no omitir el trabajo.

---

### Task 1: Migración `V16` — índices faltantes, índices duplicados y `CHECK` constraints

**Contexto del hallazgo:**
- `RepositorioEventoOutbox.findByAgregadoIdAndTipo(UUID, String)` (`compartido/infrastructure/outbox/RepositorioEventoOutbox.java:35`) no tiene índice de soporte — `outbox_eventos` (`V15__create_outbox_eventos.sql`) solo tiene el índice parcial `idx_outbox_listos` sobre `proximo_intento_en`, una tabla que crece sin límite (no hay purga todavía, eso es Fase 11).
- `ServicioReporteOrdenes` pagina `ordenes` por tienda (`RepositorioOrden.findAllByIdTienda(idTienda, pageable)`), y `ordenes` (`V5__create_orders.sql:86-89`) solo tiene índices sobre `tenant_id` solo, `(tenant_id, estado)` y `numero_orden` — falta `(tenant_id, created_at DESC)`, que es el orden natural de cualquier listado/reporte por fecha.
- `RepositorioCategoria.findAllByIdTiendaAndIdPadre(UUID, UUID)` (`catalogo/domain/RepositorioCategoria.java:16`) y `RepositorioProducto.findAllByIdTiendaAndIdCategoria(UUID, UUID, Pageable)` existen y se usan, pero `categorias`/`productos` (`V4__create_catalog.sql:74-80`) solo tienen índices de una sola columna (`idx_categories_parent` sobre `id_padre`, `idx_products_category` sobre `category_id`) en vez de compuestos con `tenant_id`.
- Índices duplicados confirmados leyendo cada migración: `idx_users_email` (`V2__create_users.sql:33`) es redundante con el índice único implícito de `usuarios.correo UNIQUE` (`V2:3`); `idx_tenants_slug` (`V3__create_tenants_and_plans.sql:47`) es redundante con `inquilinos.enlace_corto UNIQUE` (`V3:4`); `idx_cupones_tenant` (`V9__create_cupones.sql:19`) es redundante porque ya es el prefijo izquierdo del índice único de `UNIQUE(tenant_id, codigo)` (`V9:16`); `idx_cupones_codigo` (`V9:20`) no tiene ningún caller que filtre solo por `codigo` (`RepositorioCupon` solo tiene `findByIdTiendaAndCodigo`/`findAllByIdTienda`/`findByIdTiendaAndCodigoForUpdate`) — queda sin uso.
- Ningún `CHECK` protege columnas monetarias/de inventario contra un `UPDATE` directo que se salte la capa de dominio (la app ya garantiza esto vía `Producto.decreaseInventory`/`Dinero`, pero es defensa en profundidad, no redundancia con la app).
- `EntidadBase.version` (`compartido/domain/EntidadBase.java:30-32`, `@Version @Column(name = "version")`) no tiene `nullable = false`; las 21 tablas con columna `version` la declaran `BIGINT DEFAULT 0` sin `NOT NULL` en las migraciones (`V2` a `V15`). Funciona hoy porque Hibernate siempre puebla `version` al insertar (`@Version` la inicializa en 0), pero un `INSERT` que no pase por JPA (una migración de datos futura, un script) podría dejarla en `NULL` y romper el locking optimista silenciosamente.

**Files:**
- Create: `src/main/resources/db/migration/V16__indices_constraints_y_version_not_null.sql`
- Modify: `src/main/java/com/ecommerce/modulos/compartido/domain/EntidadBase.java` (agregar `nullable = false` al `@Column` de `version`, coherente con el `NOT NULL` de la migración)

**Interfaces:** No cambia ninguna firma pública. Es un cambio de esquema puro; ninguna query nueva se agrega en esta tarea (las queries que se benefician de los índices ya existen).

- [ ] **Step 1: Escribir la migración `V16`**

```sql
-- ============================================================
-- Índices faltantes (confirmados contra las queries reales de
-- cada RepositorioX; ver el plan de la Fase 10 para el detalle).
-- ============================================================
CREATE INDEX idx_outbox_agregado_tipo ON outbox_eventos (agregado_id, tipo);
CREATE INDEX idx_orders_tenant_created_at ON ordenes (tenant_id, created_at DESC);
CREATE INDEX idx_categories_tenant_parent ON categorias (tenant_id, id_padre);
CREATE INDEX idx_products_tenant_category ON productos (tenant_id, category_id);

-- ============================================================
-- Índices duplicados/sin uso — se eliminan.
-- ============================================================
-- idx_users_email es redundante: usuarios.correo ya es UNIQUE (V2), y Postgres
-- crea automáticamente un índice único para toda restricción UNIQUE.
DROP INDEX IF EXISTS idx_users_email;
-- idx_tenants_slug es redundante: inquilinos.enlace_corto ya es UNIQUE (V3).
DROP INDEX IF EXISTS idx_tenants_slug;
-- idx_cupones_tenant es redundante: ya es el prefijo izquierdo del índice único
-- de UNIQUE(tenant_id, codigo) (V9) — cualquier query que filtre solo por
-- tenant_id ya puede usar ese índice compuesto.
DROP INDEX IF EXISTS idx_cupones_tenant;
-- idx_cupones_codigo no tiene ningún caller que filtre solo por "codigo" sin
-- tenant_id (RepositorioCupon siempre filtra por idTienda + codigo juntos).
DROP INDEX IF EXISTS idx_cupones_codigo;

-- ============================================================
-- CHECK constraints — defensa en profundidad contra un UPDATE directo que
-- se salte Producto.decreaseInventory / Dinero (la app ya lo garantiza; esto
-- cubre el caso de una migración de datos o un acceso fuera de JPA).
-- ============================================================
ALTER TABLE ordenes
    ADD CONSTRAINT chk_ordenes_montos_no_negativos CHECK (
        subtotal_amount >= 0 AND monto_impuesto >= 0 AND
        monto_envio >= 0 AND total_amount >= 0
    );

ALTER TABLE order_items
    ADD CONSTRAINT chk_order_items_montos_no_negativos CHECK (
        unit_price_amount >= 0 AND subtotal_amount >= 0
    );

ALTER TABLE pagos
    ADD CONSTRAINT chk_pagos_amount_no_negativo CHECK (amount >= 0);

ALTER TABLE productos
    ADD CONSTRAINT chk_productos_amount_no_negativo CHECK (amount >= 0),
    ADD CONSTRAINT chk_productos_inventario_no_negativo CHECK (inventario >= 0);

ALTER TABLE product_variants
    ADD CONSTRAINT chk_product_variants_amount_no_negativo CHECK (amount IS NULL OR amount >= 0),
    ADD CONSTRAINT chk_product_variants_inventario_no_negativo CHECK (inventario >= 0);

ALTER TABLE cupones
    ADD CONSTRAINT chk_cupones_valor_no_negativo CHECK (valor >= 0);

-- ============================================================
-- version NOT NULL DEFAULT 0 — coherente con @Version, que Hibernate siempre
-- puebla en el insert; esto solo cierra la ventana de un insert fuera de JPA.
-- Se hace en dos pasos (backfill + NOT NULL) por si alguna fila existente ya
-- tuviera NULL en un entorno con datos reales.
-- ============================================================
UPDATE usuarios SET version = 0 WHERE version IS NULL;
UPDATE refresh_tokens SET version = 0 WHERE version IS NULL;
UPDATE inquilinos SET version = 0 WHERE version IS NULL;
UPDATE subscription_plans SET version = 0 WHERE version IS NULL;
UPDATE subscriptions SET version = 0 WHERE version IS NULL;
UPDATE direcciones SET version = 0 WHERE version IS NULL;
UPDATE ordenes SET version = 0 WHERE version IS NULL;
UPDATE order_items SET version = 0 WHERE version IS NULL;
UPDATE order_status_history SET version = 0 WHERE version IS NULL;
UPDATE categorias SET version = 0 WHERE version IS NULL;
UPDATE productos SET version = 0 WHERE version IS NULL;
UPDATE product_variants SET version = 0 WHERE version IS NULL;
UPDATE product_images SET version = 0 WHERE version IS NULL;
UPDATE pagos SET version = 0 WHERE version IS NULL;
UPDATE refunds SET version = 0 WHERE version IS NULL;
UPDATE resenas SET version = 0 WHERE version IS NULL;
UPDATE notifications SET version = 0 WHERE version IS NULL;
UPDATE cupones SET version = 0 WHERE version IS NULL;
UPDATE envios SET version = 0 WHERE version IS NULL;
UPDATE evento_tracking SET version = 0 WHERE version IS NULL;
UPDATE outbox_eventos SET version = 0 WHERE version IS NULL;

ALTER TABLE usuarios ALTER COLUMN version SET NOT NULL, ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE refresh_tokens ALTER COLUMN version SET NOT NULL, ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE inquilinos ALTER COLUMN version SET NOT NULL, ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE subscription_plans ALTER COLUMN version SET NOT NULL, ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE subscriptions ALTER COLUMN version SET NOT NULL, ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE direcciones ALTER COLUMN version SET NOT NULL, ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE ordenes ALTER COLUMN version SET NOT NULL, ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE order_items ALTER COLUMN version SET NOT NULL, ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE order_status_history ALTER COLUMN version SET NOT NULL, ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE categorias ALTER COLUMN version SET NOT NULL, ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE productos ALTER COLUMN version SET NOT NULL, ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE product_variants ALTER COLUMN version SET NOT NULL, ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE product_images ALTER COLUMN version SET NOT NULL, ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE pagos ALTER COLUMN version SET NOT NULL, ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE refunds ALTER COLUMN version SET NOT NULL, ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE resenas ALTER COLUMN version SET NOT NULL, ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE notifications ALTER COLUMN version SET NOT NULL, ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE cupones ALTER COLUMN version SET NOT NULL, ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE envios ALTER COLUMN version SET NOT NULL, ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE evento_tracking ALTER COLUMN version SET NOT NULL, ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE outbox_eventos ALTER COLUMN version SET NOT NULL, ALTER COLUMN version SET DEFAULT 0;
```

No incluir `webhook_tenants` en esta lista de `version` — esa tabla no tiene la columna todavía; la agrega la Task 2 completa (junto con el resto de las columnas de auditoría).

- [ ] **Step 2: Agregar `nullable = false` en `EntidadBase.java`**

```java
@Version
@Column(name = "version", nullable = false)
private Long version;
```

- [ ] **Step 3: Verificar que Hibernate valida el esquema resultante**

Run: `./gradlew build` (no alcanza con `test` — necesitamos que el contexto de Spring arranque contra el esquema migrado; si no hay Docker disponible para los `*IntegrationTest`, al menos `./gradlew compileJava compileTestJava` más una revisión manual de que cada `CHECK`/índice nuevo referencia columnas y tablas que existen tal cual en las migraciones anteriores — transcribir cada nombre de columna directamente desde `V4`/`V5`/`V6`/`V9`/`V15`, no de memoria).
Expected: sin errores de validación de esquema. Si hay Docker disponible, levantar el perfil de test contra Testcontainers y confirmar que `EcommerceApplicationTests` (el smoke test de arranque del contexto) pasa.

- [ ] **Step 4: Test de regresión para el `CHECK` de inventario/montos (opcional pero recomendado)**

Si hay Docker disponible, un test de integración mínimo que intente un `UPDATE productos SET inventario = -1 WHERE id = :id` directo vía `EntityManager`/`JdbcTemplate` y verifique que Postgres lo rechaza (`DataIntegrityViolationException`). Si no hay Docker, documentar el gap y dejarlo para cuando el test pueda correr en CI.

---

### Task 2: Aislamiento de tenant en `webhook_tenants`

**Contexto del hallazgo:** `WebhookTenant` (`inquilino/domain/WebhookTenant.java`) es la única entidad del proyecto que no sigue el patrón `extends EntidadInquilino` — es una clase suelta con `id`/`idTienda`/`urlDestino`/`evento`/`secret` sin auditoría ni versión, y su columna es `id_tienda` (no `tenant_id`, rompiendo la convención del resto del esquema). `RepositorioWebhookTenant` (`inquilino/domain/RepositorioWebhookTenant.java`) extiende `JpaRepository<WebhookTenant, UUID>` directo, no `RepositorioJpaBase<WebhookTenant>` — así que el filtro `@Filter("filtroInquilino")` de Hibernate (que sí protege a todas las demás entidades como defensa de fondo) nunca se activa acá. Hoy no hay ningún endpoint de administración que liste/edite webhooks por id sin pasar `idTienda` explícito, así que no hay un IDOR explotable *todavía* — pero es indirección con cero beneficio y una trampa para el próximo endpoint que se agregue sobre esta tabla.

**Files:**
- Modify: `src/main/resources/db/migration/V16__indices_constraints_y_version_not_null.sql` (agregar al final, o crear `V17` si se prefiere separar de la Task 1 — ver nota abajo)
- Modify: `src/main/java/com/ecommerce/modulos/inquilino/domain/WebhookTenant.java`
- Modify: `src/main/java/com/ecommerce/modulos/inquilino/domain/RepositorioWebhookTenant.java`
- Modify: `src/main/java/com/ecommerce/modulos/inquilino/application/ServicioEmisorWebhook.java` (si el constructor de `WebhookTenant` cambia)
- Modify: `src/test/java/com/ecommerce/modulos/inquilino/application/ServicioEmisorWebhookTest.java` (adaptar la construcción de `WebhookTenant` en los tests existentes)

**Nota sobre el número de migración:** si la Task 1 ya se integró y `V16` ya existe en la rama, esta tarea crea `V17__aislar_tenant_webhook_tenants.sql` en vez de agregar a `V16`. Verificar con `ls src/main/resources/db/migration/` antes de nombrar el archivo — Flyway falla duro si dos migraciones reclaman el mismo número de versión.

**Interfaces:**
- `WebhookTenant` pasa a `extends EntidadInquilino` (hereda `id`, `idTienda`, `version`, `creadoEn`, `actualizadoEn`, `createdBy`, `updatedBy` — mismos nombres que `Cupon`/`Categoria`).
- `RepositorioWebhookTenant` pasa a `extends RepositorioJpaBase<WebhookTenant>`.
- El método de query `buscarPorIdTiendaYEvento(UUID idTienda, String evento)` no cambia de firma (sigue filtrando por `w.idTienda`, que ahora hereda de `EntidadInquilino` en vez de ser un campo propio).

- [ ] **Step 1: Migración de esquema**

```sql
ALTER TABLE webhook_tenants RENAME COLUMN id_tienda TO tenant_id;
ALTER TABLE webhook_tenants
    ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    ADD COLUMN created_by UUID,
    ADD COLUMN updated_by UUID,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- El índice existente ya cubre (tenant_id, evento) tras el rename; Postgres actualiza
-- automáticamente el nombre de columna dentro de la definición del índice.
```

- [ ] **Step 2: Reescribir `WebhookTenant.java`**

```java
package com.ecommerce.modulos.inquilino.domain;

import com.ecommerce.modulos.compartido.domain.EntidadInquilino;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "webhook_tenants")
@Getter
@Setter
@NoArgsConstructor
public class WebhookTenant extends EntidadInquilino {

    private String urlDestino;
    private String evento;
    private String secret;

    public WebhookTenant(java.util.UUID idTienda, String urlDestino, String evento, String secret) {
        this.setIdTienda(idTienda);
        this.urlDestino = urlDestino;
        this.evento = evento;
        this.secret = secret;
    }
}
```

(Mismo patrón que `Cupon`/`Categoria`: `@Getter @Setter @NoArgsConstructor`, constructor de conveniencia que llama a `setIdTienda` heredado — no reinventar getters manuales.)

- [ ] **Step 3: Actualizar `RepositorioWebhookTenant.java`**

```java
package com.ecommerce.modulos.inquilino.domain;

import com.ecommerce.modulos.compartido.infrastructure.RepositorioJpaBase;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RepositorioWebhookTenant extends RepositorioJpaBase<WebhookTenant> {
    @Query("SELECT w FROM WebhookTenant w WHERE w.idTienda = :idTienda AND w.evento = :evento")
    List<WebhookTenant> buscarPorIdTiendaYEvento(@Param("idTienda") UUID idTienda, @Param("evento") String evento);
}
```

Verificar la firma exacta de `RepositorioJpaBase` (`compartido/infrastructure/RepositorioJpaBase.java`) antes de extenderla — debería ser genérica sobre el tipo de entidad igual que `RepositorioCupon`/`RepositorioCategoria`.

- [ ] **Step 4: Correr los tests existentes y ajustar lo que rompa**

Run: `./gradlew test --tests "com.ecommerce.modulos.inquilino.*"`
`ServicioEmisorWebhookTest.java` construye `new WebhookTenant(idTienda, url, evento, secret)` en varios tests — el constructor de conveniencia del Step 2 mantiene esa misma firma posicional, así que estos tests no deberían necesitar cambios. Si alguno falla por otra razón (por ejemplo, un mock de `save()` que dependa del tipo exacto de la entidad), documentar el ajuste en el reporte.

- [ ] **Step 5: Confirmar que el `@Filter` de Hibernate ahora protege esta tabla**

No hace falta un test nuevo dedicado si `AislamientoMultiTenantIntegrationTest` ya recorre todas las entidades `EntidadInquilino` genéricamente (revisar ese archivo primero); si es un test por-entidad explícito, agregar `WebhookTenant` a la lista si aplica. Si no hay Docker disponible para correrlo, documentar el gap.

---

### Task 3: Tuning de HikariCP en `application-prod.yml`

**Contexto del hallazgo:** `application-prod.yml` no tiene ninguna sección `spring.datasource.hikari` — corre con los defaults de Spring Boot (pool de 10 conexiones, sin `leak-detection-threshold`, sin `max-lifetime` explícito). Una query colgada (deadlock, un `SELECT ... FOR UPDATE` que nunca libera, una llamada externa dentro de una transacción) puede agotar el pool sin que nada lo detecte hasta que la aplicación entera empieza a devolver timeouts de conexión.

**Files:**
- Modify: `src/main/resources/application-prod.yml`

**Interfaces:** Ninguna — es configuración pura, no código.

- [ ] **Step 1: Agregar la sección de HikariCP**

```yaml
spring:
  jpa:
    show-sql: false
    open-in-view: false
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 2
      # Recicla conexiones periódicamente para evitar que un firewall/load balancer
      # intermedio las corte a mitad de uso sin que el pool se entere.
      max-lifetime: 1800000        # 30 min
      # Detecta (loguea) una conexión que un caller no devolvió al pool en ese tiempo —
      # típicamente una transacción que quedó abierta más de lo esperado.
      leak-detection-threshold: 60000   # 60 s
      connection-timeout: 30000    # 30 s (default de Hikari, explícito por claridad)
      data-source-properties:
        # Corta a nivel de sesión JDBC una query que se cuelga, para que no consuma
        # una conexión del pool indefinidamente.
        options: "-c statement_timeout=30000 -c lock_timeout=10000"
```

Mantener el resto de `application-prod.yml` (logging, server, management) tal cual está — solo agregar la clave `datasource` dentro de `spring`.

- [ ] **Step 2: Verificar que la sintaxis YAML es válida y que la app sigue arrancando**

Run: `./gradlew compileJava` (no afecta compilación, pero) y, si hay entorno para probarlo, `SPRING_PROFILES_ACTIVE=prod ./gradlew bootRun` contra una base de datos real, confirmando en los logs de arranque que Hikari reporta el pool configurado (`HikariPool-1 - configuration:` en el log de arranque con `maximumPoolSize=10`, etc.). Si no es posible levantar `prod` en este entorno, documentarlo — es un cambio de configuración de bajo riesgo (no toca código), pero señalarlo igual.

---

### Task 4: Mapping explícito del índice de Elasticsearch

**Contexto del hallazgo:** No existe en todo el repo ningún `CreateIndexRequest`/`indices().create(...)` — el índice `productos` de Elasticsearch se crea implícitamente por dynamic mapping la primera vez que `ServicioBusqueda.indexProduct` indexa un documento. Esto ya funciona hoy (los filtros `idTienda.keyword`/`nombreCategoria.keyword` de `ServicioBusqueda.busqueda` asumen que ES generó automáticamente el sub-campo `.keyword` para esos campos de texto, que es el comportamiento por defecto de ES para campos string), pero es frágil: si el primer documento indexado tuviera, por ejemplo, `idTienda` como un objeto en vez de string, o si una futura versión de Elasticsearch cambia el dynamic mapping por defecto, el índice completo queda con un tipo incorrecto sin que nadie lo note hasta que una búsqueda falla en producción. Además `nombre`/`descripcion` usan el analyzer `standard` genérico (dynamic default) en vez de uno que entienda español (stopwords, stemming), degradando la relevancia de `multiMatch`.

**Files:**
- Modify: `src/main/java/com/ecommerce/modulos/busqueda/infrastructure/ElasticsearchConfig.java`

**Interfaces:**
- Nuevo método privado/bean que, al arrancar la aplicación, verifica si el índice `productos` existe y, si no, lo crea con un mapping explícito. Debe ser idempotente (no fallar ni duplicar nada si el índice ya existe — importante para que los tests de integración que arrancan el contexto repetidamente no rompan).

- [ ] **Step 1: Agregar el bootstrap del índice**

Agregar a `ElasticsearchConfig.java` un `@Bean` de tipo `ApplicationRunner` (corre una vez al arrancar el contexto de Spring, después de que todos los beans están listos) que:

1. Llama a `elasticsearchClient.indices().exists(e -> e.index("productos"))`.
2. Si `.value()` es `false`, llama a `elasticsearchClient.indices().create(c -> c.index("productos").mappings(m -> m.properties(...)).settings(s -> ...))` con:
   - `idTienda`: tipo `keyword` explícito (no depender de que ES lo infiera).
   - `nombre`, `descripcion`, `nombreCategoria`: tipo `text` con `analyzer("spanish")` (el analyzer `spanish` es built-in de Elasticsearch, no requiere plugin), y un sub-campo `.keyword` (`fields(f -> f.keyword(...))`) para que seguir usando `nombreCategoria.keyword` como filtro exacto no se rompa.
   - `precio`, `calificacionPromedio`: tipo `double` (o `scaled_float` si se prefiere precisión monetaria — usar criterio, `double` es más simple y ya es como se está usando hoy vía `NumberRangeQuery`).
   - `enlaceCorto`, `urlImagen`, `id`: `keyword` (no se buscan por texto completo).
   - `conteoResenas`: `long`.

No hay código de referencia completo — la API exacta de `IndexSettings`/`TypeMapping`/`Property` del cliente 8.18.0 requiere iterar contra `./gradlew compileJava` (mismo criterio que se usó en la Fase 9 / Task 2 para `RangeQuery`). Si hace falta, extraer el jar del cache de Gradle (`~/.gradle/caches/modules-2/files-2.1/co.elastic.clients/elasticsearch-java/8.18.0/`) e inspeccionar con `javap` las clases `TypeMapping.Builder`, `Property`, `TextProperty.Builder`, `KeywordProperty.Builder` antes de escribir el código a ciegas — mismo enfoque que documentó el reporte de la Task 2 de la Fase 9.

- [ ] **Step 2: Test**

Si hay Docker disponible: un test de integración mínimo (`ElasticsearchConfigIntegrationTest` o extender `OutboxIndexacionIntegrationTest`) que arranca el contexto contra un `ElasticsearchContainer` limpio (sin el índice creado de antemano) y verifica, vía `elasticsearchClient.indices().get(...)`, que el mapping de `idTienda` es `keyword` y el de `nombre` es `text` con `analyzer: spanish`. Si no hay Docker, documentar el gap explícitamente — este es el ítem del plan más difícil de verificar sin un Elasticsearch real, así que el reporte debe ser honesto sobre qué se pudo confirmar por compilación/inspección de código y qué queda pendiente de correr en CI.

- [ ] **Step 3: Confirmar que no rompe la indexación existente**

Run: `./gradlew test --tests "com.ecommerce.modulos.busqueda.*"` — `ServicioBusquedaTest` mockea `ElasticsearchClient` así que no debería verse afectado por este cambio (el bootstrap del índice es un bean nuevo, no toca `ServicioBusqueda`). Confirmar igual que compila y que ningún test unitario nuevo hace falta ahí.

---

## Verificación final de la Fase 10

1. `./gradlew build` en verde (o con los fallos de Testcontainers documentados si no hay Docker).
2. Si hay entorno con Docker: `docker-compose -f docker/docker-compose.yml up -d`, `./gradlew bootRun`, y confirmar manualmente con `psql`/el MCP de Postgres que `\d productos`, `\d ordenes`, `\d webhook_tenants` muestran los índices/constraints/columnas nuevas, y que `GET http://localhost:9200/productos/_mapping` (si Elasticsearch está levantado) muestra el mapping explícito de la Task 4.
3. PR con título Conventional Commit (ej. `feat(compartido): indices, constraints y tenant en webhook_tenants — Fase 10`), squash-merge, CI en verde antes de mergear.
