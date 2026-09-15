# Fase 13 — Cubrir los módulos sin tests que dejó el baseline de la Fase 12

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) o superpowers:executing-plans para implementar este plan tarea por tarea. Los pasos usan sintaxis de checkbox (`- [ ]`) para seguimiento.

**Goal:** El baseline de cobertura de la Fase 12 (`docs/superpowers/plans/2026-09-13-fase-12-cobertura-baseline.md`) midió 82% de instrucciones / 71% de ramas en CI, y dejó escrita una recomendación de alcance para "Fase 13" que nunca se formalizó como plan. El `ROADMAP.md` pide 85% de cobertura. Esta fase escribe tests para los paquetes que quedaron en 0% (o casi 0%) de cobertura — `ordenes/infrastructure`, `pagos/infrastructure/pasarelas`, `resenas/infrastructure`+`domain`, `logistica/infrastructure`, `analiticas/infrastructure` — más un objetivo estirado en `carrito/domain` para acercar el número global al 85% del ROADMAP en el mismo PR.

**Architecture:** Sin cambios de arquitectura ni de código de producción — solo tests nuevos. Cada paquete objetivo es un dominio independiente (controllers/entidades sin relación entre sí), así que las 5 tareas de escritura de tests se ejecutaron en paralelo con subagentes `escritor-tests`, cada uno acotado a su propio paquete para evitar que se pisaran entre sí.

**Tech Stack:** JUnit 5, Mockito (`MockitoExtension`, `mockStatic` para el SDK de Stripe), `@WebMvcTest` + `MockMvc` para los controllers, JaCoCo (ya instrumentado desde la Fase 12) para medir el resultado.

**Spec:** Verificado en vivo contra el código antes de escribir el plan:

- Baseline real (Fase 12, medido en CI, PR #21, 481/481 tests verdes): 82% instrucciones (2.068 de 11.603 sin cubrir), 71% ramas (204 de 706 sin cubrir).
- Paquetes en 0% de cobertura: `ordenes/infrastructure` (87 instrucciones), `resenas/infrastructure` (28), `logistica/infrastructure` (17), `analiticas/infrastructure` (8), `resenas/domain` (8). `pagos/infrastructure/pasarelas` en 9.8% (83 de 92 sin cubrir).
- La matemática no cierra sola: cubrir solo los 5 paquetes en 0% (148 instrucciones) sube el global a ~83.4%; sumando `pagos/infrastructure/pasarelas` llega a ~84.2%. Para tocar el 85% real (9.863 de 11.603 instrucciones, ceil de 0.85×11.603) faltan ~328 instrucciones en total, no 148 — de ahí el objetivo estirado sobre `carrito/domain` (96 instrucciones sin cubrir en el baseline, el paquete cuyo faltante más se acerca al resto de la brecha).
- Ninguno de los 5 paquetes objetivo tenía un solo test (`*Test`/`*IntegrationTest`) antes de esta fase (confirmado con Glob sobre `src/test/java`).
- El repo ya tiene un patrón establecido y reusable para tests de controller: `ControladorPagoTest.java` (`@WebMvcTest` + `excludeFilters` sobre `FiltroInquilino`/`FiltroAutenticacionJwt`/`InterceptorLimiteTasa`/`RateLimitConfig` + `@ContextConfiguration(classes = AplicacionEcommerce.class)` + un `@TestConfiguration` que registra `AuthenticationPrincipalArgumentResolver` a mano, porque el slice nunca carga `SecurityConfig`/`@EnableWebSecurity`).

## Global Constraints

- No modificar código de producción salvo estricta necesidad de testabilidad (no hizo falta en ninguno de los 6 paquetes).
- Reusar el patrón de `@WebMvcTest` ya establecido en `ControladorPagoTest.java` — no inventar un enfoque nuevo por paquete.
- `@PreAuthorize("hasRole(...)")` no se puede re-testear en un slice `@WebMvcTest` (no carga `@EnableMethodSecurity`) — documentado como limitación conocida en cada test class que lo toca, no un gap a resolver en esta fase.
- Commits en español, Conventional Commits, sin `Co-Authored-By` de ningún asistente de IA.
- El número de cobertura real sale de CI, no de este entorno local — localmente hay 10 tests conocidos que no corren por el problema de Testcontainers/Docker Desktop ya documentado en `CLAUDE.md`, así que el número local (~74%) es artificialmente más bajo que el real de CI.

---

### Task 1: `ordenes/infrastructure` — `ControladorOrden` + `ControladorCupon`

**Files:**
- Create: `src/test/java/com/ecommerce/modulos/ordenes/infrastructure/ControladorOrdenTest.java`
- Create: `src/test/java/com/ecommerce/modulos/ordenes/infrastructure/ControladorCuponTest.java`

**Interfaces:** Ninguna — solo tests, mockean `CasoUsoOrden`, `ServicioReporteOrdenes`.

- [ ] **Step 1:** Leer `ControladorOrden.java`/`ControladorCupon.java` completos y `ControladorPagoTest.java` como referencia de patrón.
- [ ] **Step 2:** Escribir `ControladorOrdenTest` cubriendo checkout (201/400/409), listado (200/401 sin tienda propia), detalle (200/404), cancelación (200 con y sin motivo/404), reporte Excel (200/401).
- [ ] **Step 3:** Escribir `ControladorCuponTest` cubriendo alta (201/400 por validaciones `@AssertTrue`/409 duplicado/401), listado (200), cambio de estado (204/404).
- [ ] **Step 4:** Correr `./gradlew test --tests "com.ecommerce.modulos.ordenes.infrastructure.*"` — debe dar verde (21 tests).
- [ ] **Step 5: Commit** (incluido en el commit consolidado de la Task 6).

---

### Task 2: `pagos/infrastructure/pasarelas` — `ProcesadorPagoStripe`

**Files:**
- Create: `src/test/java/com/ecommerce/modulos/pagos/infrastructure/pasarelas/ProcesadorPagoStripeTest.java`

**Interfaces:** Ninguna — mockea el SDK de Stripe (`Session.create` estático) vía `Mockito.mockStatic`.

- [ ] **Step 1:** Leer `ProcesadorPagoStripe.java` y `ControladorWebhookStripeTest.java` (referencia de cómo este repo ya mockea Stripe).
- [ ] **Step 2:** Escribir tests cubriendo: identificador fijo `"STRIPE"`, checkout exitoso, parámetros enviados a Stripe (monto en centavos, moneda en minúsculas, `clientReferenceId`), referencias distintas en llamadas sucesivas, `CardException` → `ExcepcionOperacionInvalida`, `ApiConnectionException` → `ExcepcionOperacionInvalida`, `fallbackProcesar` del circuit breaker.
- [ ] **Step 3:** Revisar `StripeConfig.java` — si es config declarativa pura (sin ramas), no forzar un test artificial.
- [ ] **Step 4:** Correr `./gradlew test --tests "com.ecommerce.modulos.pagos.infrastructure.pasarelas.*"` — debe dar verde (7 tests).
- [ ] **Step 5: Commit** (incluido en el commit consolidado de la Task 6).

---

### Task 3: `resenas/infrastructure` + `resenas/domain`

**Files:**
- Create: `src/test/java/com/ecommerce/modulos/resenas/infrastructure/ControladorResenaTest.java`
- Create: `src/test/java/com/ecommerce/modulos/resenas/domain/ResenaTest.java`

**Interfaces:** Ninguna — mockea `CasoUsoCrearResena`/`CasoUsoConsultarResenas`.

- [ ] **Step 1:** Leer `ControladorResena.java`, `Resena.java`, `RepositorioResena.java`, y los tests ya existentes de `application` del mismo módulo.
- [ ] **Step 2:** Escribir `ControladorResenaTest` (paginación, creación 201, validaciones `@Valid` → 400, compra no elegible → 409) y `ResenaTest` (activa por defecto, `hide()`/`show()` e idempotencia).
- [ ] **Step 3:** `RepositorioResena` es Spring Data derivado sin lógica propia — no crear test artificial para él.
- [ ] **Step 4:** Correr `./gradlew test --tests "com.ecommerce.modulos.resenas.*"` — debe dar verde.
- [ ] **Step 5: Commit** (incluido en el commit consolidado de la Task 6).

**Nota post-implementación:** al escribir estos tests se confirmó que `GET /api/v1/productos/{id}/resenas` no está en `permitAll()` de `SecurityConfig` (a diferencia de endpoints equivalentes de catálogo/búsqueda) — cae bajo `.anyRequest().authenticated()`. Quedó fuera del alcance de esta fase (no se tocó `SecurityConfig`); se dejó como seguimiento aparte.

---

### Task 4: `logistica/infrastructure` + `analiticas/infrastructure`

**Files:**
- Create: `src/test/java/com/ecommerce/modulos/logistica/infrastructure/ControladorLogisticaTest.java`
- Create: `src/test/java/com/ecommerce/modulos/analiticas/infrastructure/ControladorAnaliticasTest.java`

**Interfaces:** Ninguna — mockean `CasoUsoLogistica`/`CasoUsoAnaliticas`.

- [ ] **Step 1:** Leer ambos controllers y los tests ya existentes de `application`/`domain` de cada módulo.
- [ ] **Step 2:** Escribir `ControladorLogisticaTest` (rastreo 200/404, actualización de estado 200/404/401) y `ControladorAnaliticasTest` (dashboard 200 con datos, 200 con mes vacío, 401 sin tienda propia).
- [ ] **Step 3:** Correr `./gradlew test --tests "com.ecommerce.modulos.logistica.infrastructure.*" --tests "com.ecommerce.modulos.analiticas.infrastructure.*"` — debe dar verde.
- [ ] **Step 4: Commit** (incluido en el commit consolidado de la Task 6).

---

### Task 5: `carrito/domain` (objetivo estirado)

**Files:**
- Create: `src/test/java/com/ecommerce/modulos/carrito/domain/CarritoTest.java`
- Create: `src/test/java/com/ecommerce/modulos/carrito/domain/ArticuloCarritoTest.java`

**Interfaces:** Ninguna — tests unitarios puros sobre POJOs (`Carrito`/`ArticuloCarrito`, backed por Redis pero sin JPA en `domain`).

- [ ] **Step 1:** Glob sobre `carrito/domain` y `carrito/application` para confirmar qué ya estaba cubierto.
- [ ] **Step 2:** Escribir `CarritoTest` cubriendo: carrito vacío, acumulación de cantidad por producto+variante, tope `MAX_ITEMS = 50`, remoción por variante exacta vs. todas las variantes (`variantId == null`), `updateQuantity` con cero/negativo, `clear()`, cálculo de subtotal/total con descuento (incluido el clamp a cero cuando el descuento supera el subtotal). `ArticuloCarritoTest` cubriendo `getSubtotal()` y `equals`/`hashCode` de Lombok.
- [ ] **Step 3:** Correr `./gradlew test --tests "com.ecommerce.modulos.carrito.domain.*"` — debe dar verde (24 tests).
- [ ] **Step 4: Commit** (incluido en el commit consolidado de la Task 6).

---

### Task 6: Housekeeping, verificación integrada y PR

**Files:**
- Modify: `CLAUDE.md` (línea de migraciones Flyway)

- [ ] **Step 1:** Corregir `CLAUDE.md` — decía "Migraciones Flyway en V1..V14", ya existen hasta V18 (V15 outbox, V16 índices/constraints, V17 aislamiento de tenant en webhooks, V18 limpieza de tablas huérfanas — trabajo de las fases 10-11 no reflejado en el doc).
- [ ] **Step 2:** Correr `./gradlew clean test` una sola vez, completo, después de integrar el trabajo de las 5 tareas anteriores (corrieron en paralelo, cada una con su propio `./gradlew test` parcial — hace falta una corrida limpia conjunta para descartar interferencia entre procesos concurrentes de Gradle sobre el mismo `build/`).

  Resultado esperado: 557 tests, 10 fallos — exactamente los ya documentados en `CLAUDE.md` (`EcommerceApplicationTests` + 9 `*IntegrationTest` con Testcontainers), cero fallos nuevos.

- [ ] **Step 3:** Commits — dos, separados por tipo:
  ```bash
  git add CLAUDE.md
  git commit -m "docs(claude): actualizar rango de migraciones Flyway a V18"

  git add src/test/java/com/ecommerce/modulos/{ordenes/infrastructure,pagos/infrastructure/pasarelas,resenas/infrastructure,resenas/domain,logistica/infrastructure,analiticas/infrastructure,carrito/domain}
  git commit -m "test(ordenes,pagos,resenas,logistica,analiticas,carrito): cubrir controladores y dominio sin tests — Fase 13"
  ```
- [ ] **Step 4:** Push y PR:
  ```bash
  git push -u origin test/cobertura-modulos-fase-13
  gh pr create --title "test(ordenes,pagos,resenas,logistica,analiticas,carrito): cubrir controladores y dominio sin tests — Fase 13" --body "..."
  ```
- [ ] **Step 5:** Esperar CI, descargar el artifact `jacoco-coverage-report` del run y documentar el resultado real en `docs/superpowers/plans/2026-09-15-fase-13-cobertura-resultado.md` (ver ese archivo).

---

## Resultado

Ejecutado íntegramente. Ver `docs/superpowers/plans/2026-09-15-fase-13-cobertura-resultado.md` para los números reales medidos en CI. PR: [#22](https://github.com/SebastianMontes-Dev/ecommerce-platform/pull/22).
