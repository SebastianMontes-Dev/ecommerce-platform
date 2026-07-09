# ADR-002: Estrategia de Multi-tenancy

## Status
Accepted

## Context
El sistema debe soportar múltiples vendedores (tenants) con datos aislados compartiendo la misma infraestructura.

## Decision
Usar **columna discriminadora (`tenant_id`)** en todas las tablas.

## Alternativas consideradas
- **Schema por tenant**: Mayor aislamiento pero Hibernate no lo soporta nativamente. Migraciones y backups complejos.
- **Base de datos por tenant**: Máximo aislamiento pero operaciones cross-tenant imposibles y costo de conexiones.
- **Columna discriminadora**: Simple, eficiente, fácil de mantener. Suficiente para la mayoría de casos.

## Implementación
- `TenantAwareEntity` agrega columna `tenant_id` a entidades
- `TenantContext` (ThreadLocal) mantiene el tenant actual
- `TenantFilter` extrae el tenant del JWT o header `X-Tenant-ID`
- Filtro de Hibernate `@Filter(name = "tenantFilter")` aplica automáticamente

## Consequences
- Aislamiento a nivel aplicación, no a nivel BD
- Validación adicional necesaria para evitar leaks de datos
- Escala bien hasta cientos de tenants sin cambios
