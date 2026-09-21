---
name: bitacora-y-contexto
description: >-
  Usar esta skill cada vez que se realice un cambio, fix, refactorización o desarrollo de una nueva funcionalidad en el proyecto, para documentar el progreso, registrar decisiones y garantizar que nunca se pierda el hilo técnico entre tareas o sesiones.
---

# Skill: Bitácora y Contexto Continuo

Esta skill asegura que cada acción técnica realizada quede documentada de forma persistente, permitiendo que cualquier agente o desarrollador retome el trabajo sin perder el contexto ni el hilo conductor.

---

## Cuándo activar esta skill

- Al iniciar una tarea medianamente compleja o de múltiples pasos (para planificar y registrar el objetivo).
- Tras implementar cambios de código (features, fixes, refactorizaciones).
- Al tomar una decisión técnica o de diseño que afecte la arquitectura.
- Al preparar el resumen final de la sesión para el usuario.

---

## Procedimiento de Documentación Continua

### 1. Documentación de Decisiones Arquitectónicas (ADR)
Si el cambio introduce o altera:
- Un framework o librería core.
- La estrategia de aislamiento de datos o seguridad.
- Un cambio de patrón de diseño (ej. Strategy para pagos, CQRS con Elasticsearch).

**Acción**: Crear un archivo `ADR-00X-<slug-en-espanol>.md` en `docs/architecture/` siguiendo el formato existente:
- **Título**: Número y nombre de la decisión.
- **Estado**: Aceptado / Propuesto / Superado.
- **Contexto**: El problema o necesidad que motivó la decisión.
- **Decisión**: Qué se eligió y por qué.
- **Consecuencias**: Ventajas y posibles trade-offs.

### 2. Actualización de Planes y Bitácora de Tareas
Para tareas complejas o planes de ejecución:
- Si existe un plan en ejecución en `docs/superpowers/plans/`, actualizar los checkboxes `- [ ]` a `- [x]` a medida que se completen pasos.
- Si se completa una épica o hito completo, mover o archivar el plan correspondiente en `docs/superpowers/plans/completados/` con la fecha en el nombre (ej. `YYYY-MM-DD-<slug>.md`).

### 3. Sincronización con la Hoja de Ruta (`ROADMAP.md`)
Si el cambio completa o avanza un punto listado en `ROADMAP.md`:
- Abrir `ROADMAP.md`.
- Marcar la casilla correspondiente con `[x]` e incluir el enlace al plan o archivo de referencia.

### 4. Commits con Trazabilidad (Conventional Commits)
- Formato: `tipo(scope): descripción en español, imperativo, sin punto final`.
- Explicar en el cuerpo del commit (si no es evidente) el **por qué** del cambio.
- **REGLA CRÍTICA**: Nunca incluir `Co-Authored-By: Claude`, `Co-Authored-By: Gemini`, `Co-Authored-By: Antigravity` ni ninguna atribución de IA.

### 5. Resumen de Cierre de Tarea (Hand-off)
Al comunicar el resultado al usuario, estructurar siempre la respuesta con:
1. **Qué se hizo**: Lista concisa de cambios implementados y archivos modificados.
2. **Decisiones clave**: Por qué se tomó ese enfoque.
3. **Validación**: Qué tests o comandos se ejecutaron y su resultado.
4. **Próximos pasos**: Qué queda pendiente o cuál es la siguiente tarea recomendada.
