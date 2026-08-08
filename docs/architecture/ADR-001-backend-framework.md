# ADR-001: Framework Backend

## Estado
Aceptado

## Contexto
Se necesita un framework backend para construir un SaaS multi-tenant de e-commerce multi-vendor.

## Decisión
Usar **Spring Boot 3.4.4 + Java 21** con arquitectura de monolito modular.

## Justificación
- Spring Boot es el estándar empresarial en Java para APIs REST
- Java 21 ofrece hilos virtuales (Virtual Threads), coincidencia de patrones (pattern matching) y mejoras de rendimiento
- El monolito modular permite un desarrollo rápido con la posibilidad de extraer microservicios
- Cada módulo sigue una Arquitectura Limpia (dominio/aplicación/infraestructura)

## Consecuencias
- Desarrollo rápido sin sobrecarga (overhead) de comunicación entre servicios
- Fácil de desplegar (un solo JAR)
- Si escala, los módulos pueden extraerse a servicios independientes
- Requiere disciplina para mantener los límites entre módulos
