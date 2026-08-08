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
1. Un módulo solo depende del núcleo compartido (`shared kernel`) y de otros módulos a través de interfaces
2. `domain` no depende de `infrastructure` ni de `application`
3. `application` depende de `domain`
4. `infrastructure` depende de `application` y `domain`
5. Comunicación entre módulos: eventos de dominio (Spring Events → RabbitMQ)

## Consecuencias
- Módulos bien definidos, fácil migración a microservicios
- El núcleo compartido evita la duplicación de código base (Objetos de Valor, excepciones)
- Sin dependencias circulares
- Pruebas aisladas por módulo
