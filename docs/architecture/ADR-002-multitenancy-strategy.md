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
- `EntidadInquilino` (`compartido/domain`) es la superclase que aporta la columna `tenant_id`
  y declara `@FilterDef(name = "filtroInquilino")` / `@Filter(condition = "tenant_id = :idTienda")`.
- `ContextoInquilino` (ThreadLocal) mantiene el `idTienda` actual del request.
- `FiltroInquilino` resuelve el `idTienda`: para dueños de tienda lo obtiene server-side
  (`ServicioResolutorInquilino`, cacheado); en su defecto, del encabezado `X-Inquilino-ID`.
  **Nunca se toma del JWT.**
- `ConfiguracionFiltroInquilinoHibernate` / `AspectoFiltroInquilino` activan el filtro por
  sesión de Hibernate como defensa de fondo.

### Sobre el enrutamiento a DB dedicada por inquilino Premium
Existió un `AbstractRoutingDataSource` (`EnrutadorFuenteDatosInquilino` + `ConfiguracionMultiTenantDB`)
como andamiaje para clientes Premium con base de datos propia, pero `determineCurrentLookupKey()`
siempre devolvía `"default"`: no enrutaba a nada y, al ser `@Primary`, desplazaba la
autoconfiguración de Spring Boot (health del datasource, métricas de Hikari, pool). Se **eliminó**
(2026-09) y se usa el `DataSource` autoconfigurado estándar. Si el aislamiento por DB dedicada
llega a ser un requisito real, reintroducir el patrón es un cambio localizado.

## Consecuencias
- Aislamiento a nivel de aplicación, no a nivel de base de datos.
- El aislamiento entre inquilinos es una **frontera de seguridad**: cada consulta, caché, evento
  y canal en tiempo real debe llevar el `idTienda`; requiere validación explícita y tests dedicados.
- Escala bien hasta cientos de inquilinos sin cambios.
