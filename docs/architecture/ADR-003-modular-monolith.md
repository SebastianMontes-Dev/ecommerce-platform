# ADR-003: Arquitectura de Monolito Modular

## Estado
Aceptado

## Contexto
El sistema tiene múltiples contextos delimitados (Identity, Tenant, Catalog, Order, Payment, etc.) que deben ser independientes pero desplegarse juntos inicialmente.

## Decisión
Usar **monolito modular con separación por paquetes** siguiendo la Arquitectura Limpia (Clean Architecture).

## Estructura de cada módulo
```
module/
├── domain/          # Entidades, Objetos de Valor, Repositorios (interfaces)
├── application/     # Casos de Uso, DTOs, Puertos
└── infrastructure/  # Controladores, Repositorios JPA, Clientes externos
```

## Reglas
1. Un módulo solo depende del núcleo compartido (`compartido/`) y de otros módulos a través de interfaces / casos de uso
2. `domain` no depende de `infrastructure` ni de `application`
3. `application` depende de `domain`
4. `infrastructure` depende de `application` y `domain`
5. Comunicación entre módulos: eventos de dominio **in-process** vía `ApplicationEventPublisher`
   (`PublicadorEventoDominio`, con `@EventListener` / `@Async`). Un broker de mensajería
   (RabbitMQ) queda como opción futura si la escala lo exige — hoy **no** está implementado.

## Consecuencias
- Módulos bien definidos, fácil migración a microservicios
- El núcleo compartido evita la duplicación de código base (Objetos de Valor, excepciones)
- Pruebas aisladas por módulo

## Estado de la implementación
El objetivo de arriba se cumple de forma parcial y se está cerrando de manera incremental
(ver auditoría / roadmap): hoy existen violaciones puntuales — inyección directa de repositorios
entre módulos (`pagos`/`notificacion` → `ordenes`), entidades de dominio anotadas con JPA/Hibernate,
y algunos controladores que exponen entidades. La transición a puertos/adaptadores es una fase
posterior del plan.
