# Fase 11 — Limpieza de esquema y housekeeping de datos

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) o superpowers:executing-plans para implementar este plan tarea por tarea. Los pasos usan sintaxis de checkbox (`- [ ]`) para seguimiento.

**Goal:** Cerrar la Fase 11 de la ETAPA 2: eliminar las dos tablas huérfanas del esquema (`refunds`, `direcciones` — decisión ya tomada con el usuario: se eliminan, no se implementan como feature) y agregar purga programada a las tres tablas de crecimiento no acotado que el proyecto ya tiene (`outbox_eventos`, `refresh_tokens`, `processed_events`), ninguna de las cuales tiene hoy ningún mecanismo de retención.

**Architecture:** Sin cambios de arquitectura. Una migración Flyway (`V18`, aditiva salvo los dos `DROP TABLE` explícitamente decididos) más un componente nuevo `ServicioPurgaDatos` (`compartido/infrastructure/purga`) con un método `@Scheduled` que corre una vez al día, siguiendo el mismo estilo que `ProcesadorOutbox` (repositorios con queries nativas de borrado masivo, sin tocar el filtro de Hibernate porque un job `@Scheduled` corre en un hilo sin `ContextoInquilino`, igual que ya documentó la Fase 10 para los listeners `@Async`).

**Tech Stack:** Java 21, Spring Boot 3.4.4, Flyway, PostgreSQL, Spring `@Scheduled`.

**Spec:** Este plan documenta su propio spec — surge de la ETAPA 2 del roadmap de continuación de NexaSaaS. Cada afirmación se verificó leyendo el código real, no la documentación:
- `refunds` (`V6__create_payments_reviews.sql:18-30`): grep de `RepositorioRefund`/`@Table(name = "refunds")` en todo `src/main/java` → cero resultados. `CasoUsoGestionarReembolso.java` opera sobre `Pago`/`Producto` (restitución de inventario), no sobre una entidad `Reembolso`. Confirmado huérfana.
- `direcciones` (`V5__create_orders.sql:1-15`): grep de `RepositorioDireccion`/`@Table(name = "direcciones")` → cero resultados. La única clase `Direccion` del proyecto (`compartido/domain/Direccion.java`) es un `@Embeddable` usado inline en `Orden` (columnas `shipping_*`/`billing_*` planas, `V5:24-35`), no mapea la tabla `direcciones`. Confirmado huérfana.
- `outbox_eventos`: sin purga — `RepositorioEventoOutbox` (`compartido/infrastructure/outbox/`) solo tiene `reclamarLote`/`countByEstado`/`findByAgregadoIdAndTipo`, nada que borre filas `PROCESADO` viejas. Ya se agregó un índice para esta tabla en la Fase 10 (`V16`), pero no una purga.
- `refresh_tokens`: sin purga — `RepositorioTokenActualizacion` (`identidad/domain/`) solo tiene `findByToken`/`findAllByUserIdAndRevokedFalse`. `TokenActualizacion.revoke()` marca `revoked=true` pero nada borra esas filas ni las expiradas (`expira_en < now`).
- `processed_events`: sin purga — `RepositorioEventoProcesado` (`pagos/domain/`) es un `JpaRepository<EventoProcesado, String>` vacío, usado por la idempotencia de webhooks de Stripe. Sin retención, crece para siempre.

## Global Constraints

- La migración `V18` es la única con un `DROP TABLE` explícito en este plan — está autorizado porque el usuario ya tomó la decisión (ver conversación: "Tablas huérfanas refunds/direcciones → se eliminan (no se implementan como feature)"). No agregar ningún otro `DROP` no discutido.
- `ddl-auto: validate`: como ninguna entidad JPA mapea `refunds`/`direcciones`, borrarlas no requiere ningún cambio de código Java — Hibernate nunca las validó porque no las conoce.
- Convención de nombres en español del proyecto para el código nuevo.
- Los valores de retención (días) van en `application.yml`/`application-prod.yml` vía `@Value` con default razonable en el código — no hardcodear números mágicos sin poder overridearlos por entorno.
- Un job `@Scheduled` corre en un hilo sin `ContextoInquilino` — un `DELETE` nativo/JPQL ahí no queda acotado por el filtro de Hibernate y borra a través de todos los tenants, que es exactamente el comportamiento deseado para housekeeping global (no hace falta ningún workaround adicional, ya lo confirmó la Fase 10 para un caso análogo).
- Commits en español, Conventional Commits, uno por tarea — sin `Co-Authored-By` de ningún asistente de IA.
- Los `*IntegrationTest` con Testcontainers pueden no correr en este entorno (sin Docker) — si es así, documentarlo, no omitir el trabajo. Si hay Docker disponible (como en varias tareas de la Fase 10), verificar contra Postgres real es preferible.

---

### Task 1: Migración `V18` — eliminar `refunds` y `direcciones`

**Files:**
- Create: `src/main/resources/db/migration/V18__eliminar_tablas_huerfanas.sql`

**Interfaces:** Ninguna — no hay entidades/repositorios que tocar, ya que ninguno mapea estas tablas.

- [ ] **Step 1: Confirmar una vez más, de forma independiente, que ninguna entidad mapea estas tablas**

Antes de escribir el `DROP`, correr (o su equivalente con las herramientas disponibles):
```
grep -rn "@Table(name = \"refunds\")\|@Table(name = \"direcciones\")\|RepositorioRefund\|RepositorioDireccion" src/main/java
```
Expected: sin resultados. Si aparece algo, DETENERSE — no continuar con el `DROP`, avisar en el reporte.

- [ ] **Step 2: Escribir la migración**

```sql
-- refunds y direcciones son tablas huerfanas: ninguna entidad JPA las mapea (verificado con
-- grep contra src/main/java antes de este DROP). CasoUsoGestionarReembolso opera sobre Pago/
-- Producto directamente, no sobre una entidad Reembolso; Direccion es un @Embeddable usado
-- inline en Orden (columnas shipping_*/billing_* planas), no mapea la tabla direcciones.
-- Decision tomada explicitamente: se eliminan, no se implementan como feature (Fase 11).
DROP TABLE IF EXISTS refunds;
DROP TABLE IF EXISTS direcciones;
```

- [ ] **Step 3: Verificar**

Run: `./gradlew build` (o, si hay Docker disponible, `docker compose -f docker/docker-compose.yml up -d postgres` + `./gradlew bootRun` contra Postgres real, confirmando en el log que Flyway aplica `V18` sin errores — mismo patrón que usaron varias tareas de la Fase 10).
Expected: sin errores. Como ninguna entidad mapea estas tablas, `ddl-auto: validate` no debería ni enterarse del cambio.

---

### Task 2: `ServicioPurgaDatos` — purga programada de `outbox_eventos`, `refresh_tokens` y `processed_events`

**Files:**
- Create: `src/main/java/com/ecommerce/modulos/compartido/infrastructure/purga/ServicioPurgaDatos.java`
- Modify: `src/main/java/com/ecommerce/modulos/compartido/infrastructure/outbox/RepositorioEventoOutbox.java` (nuevo método de borrado)
- Modify: `src/main/java/com/ecommerce/modulos/identidad/domain/RepositorioTokenActualizacion.java` (nuevo método de borrado)
- Modify: `src/main/java/com/ecommerce/modulos/pagos/domain/RepositorioEventoProcesado.java` (nuevo método de borrado)
- Modify: `src/main/resources/application.yml` y `src/main/resources/application-prod.yml` (valores de retención, opcional si los defaults de `@Value` alcanzan — usar criterio)
- Test: `src/test/java/com/ecommerce/modulos/compartido/infrastructure/purga/ServicioPurgaDatosTest.java` (nuevo, unitario con mocks)

**Interfaces:**
- `RepositorioEventoOutbox.eliminarProcesadosAntesDe(LocalDateTime antesDe) -> int` (filas borradas).
- `RepositorioTokenActualizacion.eliminarRevocadosOExpiradosAntesDe(LocalDateTime antesDe) -> int`.
- `RepositorioEventoProcesado.eliminarAntesDe(LocalDateTime antesDe) -> int`.
- `ServicioPurgaDatos.purgar()` — método público, `@Scheduled`, sin parámetros, sin valor de retorno (loguea el resultado).

- [ ] **Step 1: Escribir el test unitario que falla**

```java
package com.ecommerce.modulos.compartido.infrastructure.purga;

import com.ecommerce.modulos.compartido.infrastructure.outbox.RepositorioEventoOutbox;
import com.ecommerce.modulos.identidad.domain.RepositorioTokenActualizacion;
import com.ecommerce.modulos.pagos.domain.RepositorioEventoProcesado;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioPurgaDatosTest {

    @Mock private RepositorioEventoOutbox repositorioEventoOutbox;
    @Mock private RepositorioTokenActualizacion repositorioTokenActualizacion;
    @Mock private RepositorioEventoProcesado repositorioEventoProcesado;

    @InjectMocks
    private ServicioPurgaDatos servicioPurgaDatos;

    @Test
    void purgarDebeLlamarALosTresRepositoriosConUnCorteDeFechaEnElPasado() {
        when(repositorioEventoOutbox.eliminarProcesadosAntesDe(any())).thenReturn(3);
        when(repositorioTokenActualizacion.eliminarRevocadosOExpiradosAntesDe(any())).thenReturn(5);
        when(repositorioEventoProcesado.eliminarAntesDe(any())).thenReturn(2);

        servicioPurgaDatos.purgar();

        verify(repositorioEventoOutbox).eliminarProcesadosAntesDe(argThat(fecha -> fecha.isBefore(LocalDateTime.now())));
        verify(repositorioTokenActualizacion).eliminarRevocadosOExpiradosAntesDe(argThat(fecha -> fecha.isBefore(LocalDateTime.now())));
        verify(repositorioEventoProcesado).eliminarAntesDe(argThat(fecha -> fecha.isBefore(LocalDateTime.now())));
    }
}
```

(Ajustar el import de `argThat` — falta `import static org.mockito.ArgumentMatchers.argThat;` — agregarlo al escribir el archivo final. Este es el esqueleto, no código para copiar sin revisar.)

- [ ] **Step 2: Correr el test y verificar que falla**

Run: `./gradlew test --tests "com.ecommerce.modulos.compartido.infrastructure.purga.ServicioPurgaDatosTest"`
Expected: FAIL — no compila, `ServicioPurgaDatos` y los tres métodos de repositorio no existen todavía.

- [ ] **Step 3: Agregar los métodos de borrado a los tres repositorios**

En `RepositorioEventoOutbox.java`:
```java
@org.springframework.data.jpa.repository.Modifying
@Query(value = "DELETE FROM outbox_eventos WHERE estado = 'PROCESADO' AND procesado_en < :antesDe", nativeQuery = true)
int eliminarProcesadosAntesDe(@Param("antesDe") LocalDateTime antesDe);
```

En `RepositorioTokenActualizacion.java`:
```java
// Un token revocado no tiene ningún valor apenas se revoca -se borra sin esperar-; uno
// expirado se conserva un margen corto por si hace falta auditar un intento de uso tardío
// antes de purgarlo. Un solo corte de fecha para ambos casos simplifica la query: alcanza
// con que "antesDe" sea lo bastante viejo para cubrir el margen de los expirados.
@org.springframework.data.jpa.repository.Modifying
@Query(value = "DELETE FROM refresh_tokens WHERE revoked = true OR expira_en < :antesDe", nativeQuery = true)
int eliminarRevocadosOExpiradosAntesDe(@Param("antesDe") LocalDateTime antesDe);
```

En `RepositorioEventoProcesado.java` (agregar `import` de `Query`/`Modifying`/`Param`, hoy el archivo no los tiene):
```java
@Modifying
@Query(value = "DELETE FROM processed_events WHERE procesado_en < :antesDe", nativeQuery = true)
int eliminarAntesDe(@Param("antesDe") LocalDateTime antesDe);
```

Verificar los nombres reales de columna (`estado`, `procesado_en`, `revoked`, `expira_en`) contra `V15__create_outbox_eventos.sql`, `V2__create_users.sql` y `V8__create_processed_events.sql` antes de aplicar — no copiar a ciegas.

- [ ] **Step 4: Escribir `ServicioPurgaDatos`**

```java
package com.ecommerce.modulos.compartido.infrastructure.purga;

import com.ecommerce.modulos.compartido.infrastructure.outbox.RepositorioEventoOutbox;
import com.ecommerce.modulos.identidad.domain.RepositorioTokenActualizacion;
import com.ecommerce.modulos.pagos.domain.RepositorioEventoProcesado;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Housekeeping de las tablas de crecimiento no acotado del proyecto (ninguna tenía
 * mecanismo de retención antes de la Fase 11): outbox_eventos, refresh_tokens,
 * processed_events. Corre una vez al día por defecto; cada retención es configurable
 * por entorno.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ServicioPurgaDatos {

    private final RepositorioEventoOutbox repositorioEventoOutbox;
    private final RepositorioTokenActualizacion repositorioTokenActualizacion;
    private final RepositorioEventoProcesado repositorioEventoProcesado;

    @Value("${app.purga.outbox-dias:30}")
    private int diasRetencionOutbox;

    @Value("${app.purga.refresh-tokens-dias:7}")
    private int diasRetencionRefreshTokens;

    @Value("${app.purga.eventos-procesados-dias:90}")
    private int diasRetencionEventosProcesados;

    @Scheduled(cron = "${app.purga.cron:0 0 3 * * *}")
    @Transactional
    public void purgar() {
        LocalDateTime ahora = LocalDateTime.now();
        int outbox = repositorioEventoOutbox.eliminarProcesadosAntesDe(ahora.minusDays(diasRetencionOutbox));
        int tokens = repositorioTokenActualizacion.eliminarRevocadosOExpiradosAntesDe(ahora.minusDays(diasRetencionRefreshTokens));
        int eventos = repositorioEventoProcesado.eliminarAntesDe(ahora.minusDays(diasRetencionEventosProcesados));
        log.info("Purga de datos: {} eventos de outbox, {} refresh tokens, {} eventos procesados eliminados",
                outbox, tokens, eventos);
    }
}
```

Confirmar que `@EnableScheduling` ya está presente en el proyecto (lo requiere `ProcesadorOutbox.procesar()`, que ya usa `@Scheduled` — buscar la clase de configuración con `grep -rn "@EnableScheduling" src/main/java`) — si ya está, no hace falta agregarlo de nuevo.

- [ ] **Step 5: Correr el test y verificar que pasa**

Run: `./gradlew test --tests "com.ecommerce.modulos.compartido.infrastructure.purga.ServicioPurgaDatosTest"`
Expected: PASS.

- [ ] **Step 6: Verificación de integración (si hay Docker disponible)**

Insertar manualmente (vía `psql` o el MCP de Postgres) una fila vieja en cada una de las tres tablas (una en `outbox_eventos` con `estado='PROCESADO'` y `procesado_en` de hace 40 días, una en `refresh_tokens` con `revoked=true`, una en `processed_events` con `procesado_en` de hace 100 días), invocar `ServicioPurgaDatos.purgar()` manualmente (por ejemplo desde un test de integración, o exponiendo el bean y llamándolo desde un endpoint temporal de prueba que después se borra), y confirmar que las tres filas desaparecen y que una fila reciente de cada tabla sobrevive. Si no hay Docker, documentar el gap — el test unitario con mocks ya cubre la lógica de orquestación (qué se llama con qué corte de fecha), lo que falta verificar es la query SQL nativa en sí.

---

## Verificación final de la Fase 11

1. `./gradlew build` en verde (o con los fallos de Testcontainers documentados si no hay Docker).
2. Si hay Docker: confirmar con `\dt` en `psql` que `refunds`/`direcciones` ya no existen, y que la purga programada corre sin errores en los logs de `bootRun` (esperar al cron, o invocar el método manualmente en un test).
3. PR con título Conventional Commit (ej. `chore(compartido): eliminar tablas huerfanas y agregar purga de datos — Fase 11`), squash-merge, CI en verde antes de mergear.
