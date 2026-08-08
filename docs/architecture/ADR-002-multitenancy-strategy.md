# ADR-002: Estrategia Multiusuario (Multi-tenancy)

## Estado
Aceptado

## Contexto
El sistema debe soportar múltiples vendedores (tenants) con datos aislados compartiendo la misma infraestructura.

## Decisión
Usar **columna discriminadora (`tenant_id`)** en todas las tablas.

## Alternativas consideradas
- **Esquema por inquilino (tenant)**: Mayor aislamiento pero Hibernate no lo soporta nativamente. Migraciones y respaldos complejos.
- **Base de datos por inquilino (tenant)**: Máximo aislamiento pero operaciones entre tenants son imposibles y costo alto de conexiones.
- **Columna discriminadora**: Simple, eficiente, fácil de mantener. Suficiente para la mayoría de los casos.

## Implementación
- `TenantAwareEntity` agrega la columna `tenant_id` a las entidades
- `TenantContext` (ThreadLocal) mantiene el tenant actual
- `TenantFilter` extrae el tenant del JWT o del encabezado `X-Tenant-ID`
- El filtro de Hibernate `@Filter(name = "tenantFilter")` se aplica automáticamente

## Consecuencias
- Aislamiento a nivel de aplicación, no a nivel de base de datos
- Validación adicional necesaria para evitar fugas de datos
- Escala bien hasta cientos de inquilinos sin cambios
