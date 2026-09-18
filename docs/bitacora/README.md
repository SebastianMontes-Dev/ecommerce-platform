# 📝 Bitácora de Cambios y Registro de Contexto

Este directorio almacena el historial cronológico y continuo de cambios, decisiones y estado de las tareas desarrolladas en **NexaSaaS**.

## Convención de Registro

Cada entrada en esta bitácora debe seguir el formato:

```markdown
### [YYYY-MM-DD] - [Título Corto de la Tarea / Cambio]
- **Módulo(s)**: `nombre-del-modulo` (ej. `pagos`, `catalogo`, `inquilino`, etc.)
- **Descripción**: Resumen del objetivo y lo que se implementó o corrigió.
- **Archivos modificados/creados**:
  - `ruta/al/archivo1`
  - `ruta/al/archivo2`
- **Decisiones técnicas**: Rationale o motivos detrás de la solución.
- **Validación realizada**: Tests ejecutados (`./gradlew test`), comandos o endpoints probados.
- **Próximos pasos**: Tareas derivadas o siguientes pasos.
```

---

## Entradas Recientes

### [2026-09-10] - Estandarización de Reglas para Agentes (AGENTS.md) y Configuración de Skills
- **Módulo(s)**: `compartido`, `tooling`, `documentación`
- **Descripción**: Migración y adaptación de directrices desde `CLAUDE.md` a `AGENTS.md` a nivel global y de proyecto. Implementación de regla estricta de documentación continua obligatoria y creación de skills de Antigravity para memoria y desarrollo backend.
- **Archivos modificados/creados**:
  - `C:\Users\sabas\Documentos\AGENTS.md` (reglas transversales)
  - `c:\Users\sabas\Documentos\ecommerce-platform\AGENTS.md` (reglas y contexto de proyecto)
  - `c:\Users\sabas\Documentos\ecommerce-platform\.agents\skills\bitacora-y-contexto\SKILL.md` (skill de documentación)
  - `c:\Users\sabas\Documentos\ecommerce-platform\.agents\skills\desarrollo-backend-spring\SKILL.md` (skill de backend)
  - `docs/bitacora/README.md` (este registro de bitácora)
- **Decisiones técnicas**: Utilización del sistema nativo de customizaciones de Antigravity (`.agents/skills/`) con YAML frontmatter y descubrimiento automático.
- **Validación realizada**: Verificación de existencia de archivos, permisos y sintaxis.
- **Próximos pasos**: Completar la instalación del conjunto integral de skills de backend y aseguramiento de calidad.

### [2026-09-10] - Despliegue de Suite Integral de 7 Skills Backend para Antigravity
- **Módulo(s)**: `tooling`, `calidad`, `infraestructura`, `persistencia`, `testing`
- **Descripción**: Instalación de la suite completa de 7 skills modulares en `.agents/skills/` para asistir integralmente en memoria de tareas, desarrollo de arquitectura DDD en Spring Boot, pruebas automáticas con Testcontainers, auditoría de seguridad, migraciones Flyway, operación de contenedores Docker y flujo Git sin trailers de IA.
- **Archivos modificados/creados**:
  - `c:\Users\sabas\Documentos\ecommerce-platform\.agents\skills\testing-y-testcontainers\SKILL.md`
  - `c:\Users\sabas\Documentos\ecommerce-platform\.agents\skills\auditoria-seguridad-y-calidad\SKILL.md`
  - `c:\Users\sabas\Documentos\ecommerce-platform\.agents\skills\migraciones-flyway-y-db\SKILL.md`
  - `c:\Users\sabas\Documentos\ecommerce-platform\.agents\skills\infraestructura-y-docker\SKILL.md`
  - `c:\Users\sabas\Documentos\ecommerce-platform\.agents\skills\flujo-git-y-entregas\SKILL.md`
  - `c:\Users\sabas\Documentos\ecommerce-platform\AGENTS.md`
- **Decisiones técnicas**: Desacoplamiento modular de skills bajo el estándar Antigravity con progressive disclosure, facilitando que el agente cargue el contexto especializado solo cuando la tarea lo amerite.
- **Validación realizada**: Comprobación de frontmatter YAML, sintaxis Markdown y referencias a ficheros.
- **Próximos pasos**: Utilizar las skills activas en la ejecución de los siguientes ítems de la Fase 1 del Roadmap.

### [2026-09-15] - Fase 13: cobertura de tests en módulos sin cubrir + hallazgo en analíticas
- **Módulo(s)**: `ordenes`, `pagos`, `resenas`, `logistica`, `analiticas`, `carrito`
- **Descripción**: Retomada la numeración de fases desde el baseline de cobertura de la Fase 12 (82%/71% en CI, sin plan formal de Fase 13). Se escribieron tests para los 5 paquetes que quedaron en 0% de cobertura (`ordenes/infrastructure`, `resenas/infrastructure`+`domain`, `logistica/infrastructure`, `analiticas/infrastructure`) más `pagos/infrastructure/pasarelas` (9.8%) y, como objetivo estirado, `carrito/domain` (45.1%) — los 7 quedaron en 100% de cobertura de instrucciones. Cobertura global subió de 82%→84% instrucciones, 71%→74% ramas (a 1 instrucción del 85% exacto del ROADMAP). Ver plan y resultado completos en `docs/superpowers/plans/2026-09-15-fase-13-cobertura-modulos-restantes.md` y `docs/superpowers/plans/2026-09-15-fase-13-cobertura-resultado.md`.
- **Archivos modificados/creados**:
  - `src/test/java/com/ecommerce/modulos/ordenes/infrastructure/ControladorOrdenTest.java`, `ControladorCuponTest.java`
  - `src/test/java/com/ecommerce/modulos/pagos/infrastructure/pasarelas/ProcesadorPagoStripeTest.java`
  - `src/test/java/com/ecommerce/modulos/resenas/infrastructure/ControladorResenaTest.java`, `src/test/java/com/ecommerce/modulos/resenas/domain/ResenaTest.java`
  - `src/test/java/com/ecommerce/modulos/logistica/infrastructure/ControladorLogisticaTest.java`
  - `src/test/java/com/ecommerce/modulos/analiticas/infrastructure/ControladorAnaliticasTest.java`
  - `src/test/java/com/ecommerce/modulos/carrito/domain/CarritoTest.java`, `ArticuloCarritoTest.java`
  - `CLAUDE.md` (corrección: migraciones Flyway V1..V14 → V1..V18)
- **Decisiones técnicas**: Los 5 conjuntos de tests se escribieron en paralelo (subagentes `escritor-tests`, uno por paquete, sin estado compartido) reusando el patrón `@WebMvcTest` ya establecido en `ControladorPagoTest` y el `mockStatic` sobre el SDK de Stripe ya usado en `ControladorWebhookStripeTest`. No se tocó código de producción. No se forzó un test artificial solo para cruzar el 85% mostrado por JaCoCo (quedó a 1 instrucción exacta del corte) — se prioriza cobertura con propósito real sobre perseguir el número.
- **Validación realizada**: `./gradlew clean test` local (557 tests, 10 fallos — todos preexistentes por Testcontainers/Docker Desktop, cero regresiones nuevas) y CI en verde sobre [PR #22](https://github.com/SebastianMontes-Dev/ecommerce-platform/pull/22) (run `34970377443`), con el reporte JaCoCo descargado y verificado paquete por paquete.
- **Hallazgo relacionado (fuera de alcance, no corregido en esta fase)**: revisando una observación externa sobre `analiticas` (sugerencias de índices, `generate_series` para rellenar días sin ventas, `LocalDate` en vez de `TO_CHAR`, cache Redis) se confirmó un bug real: `CasoUsoAnaliticas` usa la columna `creado_en` en sus 3 queries SQL, pero esa columna no existe en `ordenes` (la real es `created_at`, nunca renombrada en las 18 migraciones Flyway) — el test de esa clase mockea `JdbcTemplate` por completo, así que nunca se detectó. Evaluación completa de la observación (qué partes son correctas, cuáles ya estaban parcialmente resueltas, y por qué no aplica meter una cola de mensajería) discutida en esta misma sesión; no se implementó nada todavía.
- **Próximos pasos**: (1) decidir si mergear el PR #22; (2) corregir el bug de `creado_en`/`created_at` en `analiticas` (prioridad alta, bug real) junto con las mejoras de query/cache evaluadas; (3) continuar el plan pendiente de agregar gitleaks/Trivy/SpotBugs a CI (research iniciada, plan no terminado); (4) tarea de fondo en curso investigando por qué `GET /resenas` no es público en `SecurityConfig`.

