# Fase 13 — Resultado de cobertura de tests

Medido con JaCoCo sobre la corrida de CI del PR de esta fase ([PR #22](https://github.com/SebastianMontes-Dev/ecommerce-platform/pull/22), run [`34970377443`](https://github.com/SebastianMontes-Dev/ecommerce-platform/actions/runs/34970377443), commit `d5c6641`), CI en verde (a diferencia de este entorno local, donde 10 tests no corren — ver `CLAUDE.md` sección Testing).

## Número global

- Cobertura de instrucciones: **84%** (1.741 de 11.603 instrucciones sin cubrir) — sube desde el 82% de la Fase 12.
- Cobertura de ramas (branches): **74%** (179 de 706 ramas sin cubrir) — sube desde el 71% de la Fase 12.

## Los 7 paquetes objetivo de esta fase

Confirmado en el reporte de CI, los 7 paquetes que eran el alcance de la Fase 13 quedaron los 7 en **100% de cobertura de instrucciones**:

| Paquete | Antes (Fase 12) | Después (Fase 13) |
|---|---|---|
| `modulos/ordenes/infrastructure` | 0% | **100%** |
| `modulos/pagos/infrastructure/pasarelas` | 9.8% | **100%** |
| `modulos/resenas/infrastructure` | 0% | **100%** |
| `modulos/resenas/domain` | 0% | **100%** |
| `modulos/logistica/infrastructure` | 0% | **100%** |
| `modulos/analiticas/infrastructure` | 0% | **100%** |
| `modulos/carrito/domain` (objetivo estirado) | 45.1% | **100%** |

## Brecha hacia el 85% del ROADMAP

Cálculo exacto: 85% de 11.603 instrucciones = 9.862,55 → hace falta cubrir **9.863** para tocar el 85% redondeado hacia arriba. Esta fase dejó **9.862** instrucciones cubiertas (11.603 − 1.741) — **a 1 sola instrucción** del corte exacto. JaCoCo muestra el número redondeado hacia abajo (84%) porque 9.862/11.603 = 84.9996...%, que trunca a 84% en vez de redondear a 85%.

En términos prácticos: el ROADMAP está prácticamente cumplido. Cualquier test adicional, en cualquier paquete, cruza el 85% real. Los paquetes con más instrucciones sin cubrir que quedan como candidatos para una fase futura (no necesaria para cerrar el ROADMAP, ya casi cerrado) son `identidad/application` (419), `identidad/domain` (152), `ordenes/domain/events` (100, 98% ya) y `busqueda/domain` (98).

## Decisión sobre el 1% restante

No se agregó un test adicional "de relleno" solo para forzar el redondeo a 85% mostrado — el número real ya está prácticamente en el objetivo y agregar un test sin una razón de cobertura genuina iría en contra del criterio de esta fase (cubrir paquetes de negocio realmente desnudos, no perseguir un porcentaje). Si en una fase futura se agregan tests con propósito propio en `identidad`, `ordenes/domain/events` o `busqueda/domain`, el 85% real quedará superado como efecto colateral.

## Hallazgo relacionado (fuera de alcance de esta fase)

Durante la revisión de una observación externa sobre el módulo `analiticas` (ver `docs/bitacora/README.md`, entrada del 2026-09-15) se detectó que `CasoUsoAnaliticas` usa la columna `creado_en` en sus 3 queries SQL, columna que **no existe** en la tabla `ordenes` (la columna real es `created_at`, nunca renombrada — confirmado revisando las 18 migraciones Flyway). El test de esta clase mockea `JdbcTemplate` por completo, así que el bug nunca se detectó en CI. No es parte del alcance de la Fase 13 (que solo agregó tests, sin tocar producción) — queda documentado como bug real a corregir en una fase propia.
