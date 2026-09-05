# Guía de contribución

Convenciones para commits, ramas y Pull Requests en este repositorio. El objetivo es que el historial de `master` sea legible y predecible, sin importar quién (o qué herramienta) escriba el código.

## Commits

Formato: [Conventional Commits](https://www.conventionalcommits.org/) con descripción en español.

```
tipo(scope): descripción en español, imperativo, sin punto final

Cuerpo opcional: explica el POR QUÉ del cambio, no el qué — el diff ya
dice qué cambió. Usalo cuando el motivo no sea obvio (un bug no evidente,
una restricción externa, una decisión de diseño no trivial).
```

**Tipos** (siempre en inglés, son parte del estándar):

| Tipo | Uso |
|---|---|
| `feat` | Funcionalidad nueva |
| `fix` | Corrección de un bug |
| `docs` | Solo documentación (README, ROADMAP, comentarios) |
| `refactor` | Cambio de estructura interna sin alterar comportamiento |
| `test` | Agregar o corregir tests, sin tocar código de producción |
| `chore` | Mantenimiento (dependencias, configuración, tooling) |
| `perf` | Mejora de rendimiento |
| `style` | Formato/estilo sin efecto en lógica |
| `build` | Cambios en el sistema de build (Gradle, Docker) |
| `ci` | Cambios en workflows de GitHub Actions |
| `revert` | Revertir un commit anterior |

**Scope** (opcional, entre paréntesis): el nombre del módulo tal cual la carpeta bajo `src/main/java/com/ecommerce/modulos/` — `identidad`, `inquilino`, `catalogo`, `busqueda`, `carrito`, `ordenes`, `pagos`, `notificacion`, `logistica`, `resenas`, `analiticas`, `ia`, `compartido`. Siempre en español y en singular/plural igual que la carpeta (ej. `ordenes`, no `order` ni `orden`).

Ejemplos:
```
feat(pagos): agregar pasarela de MercadoPago
fix(ordenes): validar que el cliente autenticado sea dueño de la orden
docs: actualizar ROADMAP con la fase de internacionalización
chore: actualizar gradle-wrapper a 8.13
```

Nunca agregar `Co-Authored-By: Claude` (ni ninguna variante de atribución a la IA) — los commits y PRs quedan únicamente bajo la cuenta del autor humano.

## Ramas

```
tipo/slug-corto-en-espanol-kebab-case
```

Mismos `tipo` que los commits (`feat`, `fix`, `chore`, `docs`, `refactor`, `test`, `ci`). El slug es un resumen de 2-4 palabras en español, minúsculas, separadas por guiones, sin tildes.

Ejemplos: `feat/checkout-multipasarela`, `fix/aislamiento-multi-tenant`, `chore/actualizar-dependencias`.

## Pull Requests

- **Título**: mismo formato Conventional Commit que los commits (`tipo(scope): descripción`). Con squash-merge, el título del PR se convierte en el mensaje del commit final en `master`, así que debe seguir el mismo estándar.
- **Descripción**: usar la plantilla en [`.github/pull_request_template.md`](.github/pull_request_template.md) (Resumen / Cambios / Plan de pruebas).
- **Merge strategy**: squash-merge por defecto, para mantener un commit por PR en `master`. Usar merge commit solo si hay una razón explícita para preservar el historial granular de la rama.
- **CI en verde** antes de mergear — no mergear con checks en rojo o pendientes.

## Idioma

Nombres de clases, commits, ramas y PRs en español (siguiendo la convención ya establecida del código: `ServicioX`, `CasoUsoX`, `ControladorX`, `RepositorioX`). Los tipos de Conventional Commits (`feat`, `fix`, etc.) se mantienen en inglés porque son parte del estándar y de la integración con herramientas (changelogs automáticos, labels de PR, etc.).
