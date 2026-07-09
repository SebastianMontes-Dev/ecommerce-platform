# ADR-001: Framework Backend

## Status
Accepted

## Context
Se necesita un framework backend para construir un SaaS multi-tenant de e-commerce multi-vendor.

## Decision
Usar **Spring Boot 3.4.4 + Java 21** con arquitectura de monolito modular.

## Rationale
- Spring Boot es el estándar empresarial en Java para APIs REST
- Java 21 ofrece Virtual Threads, pattern matching y mejoras de rendimiento
- El monolito modular permite desarrollo rápido con posibilidad de extraer microservicios
- Cada módulo sigue Clean Architecture (domain/application/infrastructure)

## Consequences
- Desarrollo rápido sin overhead de comunicación entre servicios
- Fácil de deployar (un solo JAR)
- Si escala, módulos pueden extraerse a servicios independientes
- Requiere disciplina para mantener límites entre módulos
