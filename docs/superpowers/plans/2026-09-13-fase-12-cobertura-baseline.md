# Fase 12 — Baseline de cobertura de tests

Medido con JaCoCo sobre la corrida de CI del PR de esta fase ([PR #21](https://github.com/SebastianMontes-Dev/ecommerce-platform/pull/21), run [`34806366814`](https://github.com/SebastianMontes-Dev/ecommerce-platform/actions/runs/34806366814), commit `dfc894d`), 481/481 tests verdes (a diferencia de este entorno local, donde 10 no corren — ver `CLAUDE.md` sección Testing).

## Número global

- Cobertura de instrucciones: **82%** (2.068 de 11.603 instrucciones sin cubrir)
- Cobertura de ramas (branches): **71%** (204 de 706 ramas sin cubrir)

## Paquetes con menor cobertura (candidatos para una fase futura)

| Paquete | Cobertura de instrucciones | Instrucciones sin cubrir |
|---|---|---|
| `modulos/ordenes/infrastructure` | **0%** | 87 |
| `modulos/resenas/infrastructure` | **0%** | 28 |
| `modulos/logistica/infrastructure` | **0%** | 17 |
| `modulos/analiticas/infrastructure` | **0%** | 8 |
| `modulos/resenas/domain` | **0%** | 8 |
| `modulos/pagos/infrastructure/pasarelas` | 9.8% | 83 de 92 |
| `modulos/catalogo/infrastructure` | 23.4% | 291 de 380 |
| `bootstrap` | 37.5% | 5 de 8 |
| `modulos/carrito/application` | 40.4% | 202 de 339 |
| `modulos/carrito/domain` | 45.1% | 96 de 175 |

`modulos/ordenes/infrastructure` es el hallazgo más notable: es el paquete con más instrucciones sin cubrir entre los que están en **0%** (87), a pesar de ser un dominio central del negocio (órdenes) — no es, sin embargo, el paquete con más instrucciones sin cubrir de *todo* el proyecto: `catalogo/infrastructure` (291) y `carrito/application` (202) tienen más, aunque con cobertura parcial, no nula. `modulos/pagos/infrastructure/pasarelas` (el patrón Strategy de pasarelas de pago, Stripe) también llama la atención por lo bajo (9.8%) dado que maneja dinero real.

## Brecha hacia el 85% del ROADMAP

El número global de instrucciones (82%) ya está cerca del 85% pedido por el `ROADMAP.md` — una brecha de solo ~3 puntos porcentuales en total. Esa brecha global no está repartida parejo: hay cinco paquetes completamente en 0% (87+28+17+8+8 = 148 instrucciones sin cubrir entre los cinco, ~7% de las 2.068 totales) que son un objetivo claro y acotado para cerrar antes que perseguir cobertura repartida en el resto del proyecto. La cobertura de ramas (71%) tiene una brecha bastante mayor y más repartida que la de instrucciones, y no está pedida explícitamente por el ROADMAP pero es una señal de calidad real de los tests existentes (¿cubren los casos negativos, no solo el happy path?).

## Siguiente paso sugerido

Fase 13 (a definir): escribir tests dirigidos primero a `modulos/ordenes/infrastructure` (0%, el paquete más grande sin cubrir, dominio de negocio central) y `modulos/pagos/infrastructure/pasarelas` (9.8%, maneja pagos reales) — esos dos por criticidad de negocio, no solo por tamaño. Los otros tres paquetes en 0% (`resenas/infrastructure`, `logistica/infrastructure`, `analiticas/infrastructure`) son más chicos (8-28 instrucciones cada uno) y podrían resolverse rápido en la misma fase. No incluido en el alcance de la Fase 12.
