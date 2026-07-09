# ADR-003: Arquitectura de Monolito Modular

## Status
Accepted

## Context
El sistema tiene múltiples bounded contexts (Identity, Tenant, Catalog, Order, Payment, etc.) que deben ser independientes pero desplegarse juntos inicialmente.

## Decision
Usar **monolito modular con separación por paquetes** siguiendo Clean Architecture.

## Estructura de cada módulo
```
module/
├── domain/          # Entidades, Value Objects, Repositorios (interfaces)
├── application/     # Use Cases, DTOs, Puertos
└── infrastructure/  # Controllers, JPA Repos, Clientes externos
```

## Reglas
1. Un módulo solo depende del `shared kernel` y de otros módulos a través de interfaces
2. `domain` no depende de `infrastructure` ni de `application`
3. `application` depende de `domain`
4. `infrastructure` depende de `application` y `domain`
5. Comunicación entre módulos: eventos de dominio (Spring Events → RabbitMQ)

## Consequences
- Módulos bien definidos, fácil migración a microservicios
- Shared kernel evita duplicación de código base (VOs, excepciones)
- Sin dependencias circulares
- Testing aislado por módulo
