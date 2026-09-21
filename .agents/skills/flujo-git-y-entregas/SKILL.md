---
name: flujo-git-y-entregas
description: >-
  Usar esta skill al preparar ramas, redactar mensajes de commit (Conventional Commits en español), abrir Pull Requests o estructurar entregas de código en ecommerce-platform, garantizando cero atribución de IA.
---

# Skill: Flujo Git y Entregas (NexaSaaS)

Esta skill garantiza que todo el historial de Git del repositorio sea limpio, semántico, trazable y cumpla estrictamente con [`CONTRIBUTING.md`](file:///c:/Users/sabas/Documentos/ecommerce-platform/CONTRIBUTING.md).

---

## 1. Reglas de Ramas

Formato obligatorio:
```text
tipo/slug-corto-en-espanol-kebab-case
```

- **Tipos permitidos**: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`, `perf`, `build`, `ci`.
- **Slug**: 2 a 4 palabras en minúsculas, separadas por guiones, en español, sin acentos ni caracteres especiales.
- **Ejemplos**:
  - `feat/pago-con-mercadopago`
  - `fix/filtro-inquilino-sesion`
  - `chore/actualizar-dependencias-gradle`

---

## 2. Convención de Commits (Conventional Commits)

Formato obligatorio:
```text
tipo(scope): descripción en español, imperativo, sin punto final

Cuerpo opcional explicando el POR QUÉ del cambio si no es evidente.
```

### Scopes Válidos (Módulos exactos en singular/plural)
`identidad`, `inquilino`, `catalogo`, `busqueda`, `carrito`, `ordenes`, `pagos`, `notificacion`, `logistica`, `resenas`, `analiticas`, `ia`, `compartido`.

### Ejemplos Correctos:
- `feat(pagos): agregar pasarela de MercadoPago`
- `fix(ordenes): validar que el cliente autenticado sea dueño de la orden`
- `refactor(catalogo): extraer logica de variantes a caso de uso dedicado`
- `docs: actualizar ROADMAP con la fase de internacionalización`

---

## 3. Prohibición Total de Trailers de IA

> [!CRITICAL]
> **Nunca agregar trailers de autoría a IA:**
> - Jamás agregar líneas como `Co-Authored-By: Claude`, `Co-Authored-By: Gemini`, `Co-Authored-By: Antigravity`, `Co-Authored-By: Assistant` o similares.
> - Los commits y PRs deben registrarse exclusivamente bajo la cuenta y autoría del usuario humano.

---

## 4. Plantilla de Pull Request

Al redactar o preparar un Pull Request, emplear la estructura definida en [`.github/pull_request_template.md`](file:///c:/Users/sabas/Documentos/ecommerce-platform/.github/pull_request_template.md):

```markdown
## Resumen
Breve resumen del objetivo y necesidad del cambio.

## Cambios realizados
- Detalle 1
- Detalle 2

## Plan de pruebas
- [x] Pruebas unitarias ejecutadas (`./gradlew test`)
- [ ] Pruebas de integración con Testcontainers
- [ ] Validación manual de endpoints con Swagger UI o curl

## Checklist
- [x] Rama nombrada según la convención
- [x] Commits en Conventional Commits en español
- [x] Sin secretos en el código
- [x] Sin trailers de atribución a IA
```
