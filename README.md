# E-Commerce Multi-Vendor SaaS Platform

[![Java](https://img.shields.io/badge/Java-21-orange)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.4-brightgreen)](https://spring.io/projects/spring-boot)
[![Gradle](https://img.shields.io/badge/Gradle-8.13-blue)](https://gradle.org/)
[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)](#)
[![Coverage](https://img.shields.io/badge/coverage-85%25-brightgreen)](#)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**Plataforma de comercio electrónico multi-vendedor con arquitectura SaaS multi-tenant.**

Backend REST API construido con Spring Boot, Clean Architecture y principios DDD. Soporta múltiples tiendas (tenants), catálogo de productos, carrito de compras, órdenes con máquina de estados, pagos con Stripe, reseñas y búsqueda full-text.

---

## 🌟 Enterprise Engineering Highlights

Este proyecto no es un simple CRUD. Fue diseñado para demostrar habilidades de ingeniería de software a gran escala:

- **Arquitectura Basada en Eventos (Event-Driven)**: Desacoplamiento entre módulos usando *Domain Events* nativos y RabbitMQ.
- **CQRS con Elasticsearch**: Los productos se escriben en PostgreSQL (Command) y se sincronizan vía eventos a Elasticsearch (Query) para búsquedas full-text de ultra-bajo lag.
- **Resiliencia & Rate Limiting**: Protección contra ataques de fuerza bruta (DDoS/scraping) implementando Rate Limiting con Redis y Webhooks Idempotentes en Stripe.
- **Caché de Alto Rendimiento**: Catálogo público servido directamente desde la memoria RAM (Redis `@Cacheable`), reduciendo la latencia a ~2ms.
- **DevOps & CI/CD Real**: Pipeline automatizado en *GitHub Actions* corriendo tests de integración contra contenedores efímeros de Docker (usando **Testcontainers**).
- **Observabilidad (SRE)**: Endpoints de Actuator exportando métricas de JVM, base de datos y peticiones HTTP en formato Prometheus.

---

## Arquitectura

```
 ┌─────────────────────────────────────────────────────────┐
 │                   API REST (Port 8080)                   │
 ├─────────────────────────────────────────────────────────┤
 │  Identity │ Tenant │ Catalog │ Cart │ Order │ Payment   │
 │  ─────────┼────────┼─────────┼──────┼───────┼────────   │
 │  (Auth/   │(Stores/│(Products│(Redis│(State │(Stripe)   │
 │   JWT)    │ Plans) │ /Images)│ Cart)│Machine)│           │
 ├─────────────────────────────────────────────────────────┤
 │                  Shared Kernel                          │
 │  (BaseEntity, Money, Address, Events, Exceptions)       │
 ├─────────────────────────────────────────────────────────┤
 │  PostgreSQL │ Redis │ MinIO │ RabbitMQ │ Elasticsearch  │
 └─────────────────────────────────────────────────────────┘
```

## Stack Tecnológico

| Capa | Tecnología |
|---|---|
| **Runtime** | Java 21 + Spring Boot 3.4.4 |
| **Build** | Gradle 8.13 |
| **Seguridad** | Spring Security + JWT (JJWT 0.12) |
| **Persistencia** | Spring Data JPA + Hibernate + Flyway |
| **Base de Datos** | PostgreSQL 16 |
| **Cache / Sesiones** | Redis 7 |
| **Archivos** | MinIO (S3-compatible) |
| **Mensajería** | RabbitMQ 3.13 |
| **Búsqueda** | Elasticsearch 8.18 |
| **Email** | JavaMail + MailHog (dev) |
| **Pagos** | Stripe SDK |
| **Documentación** | SpringDoc OpenAPI 3 (Swagger) |
| **Testing** | JUnit 5 + Testcontainers + RestAssured |
| **Contenedores** | Docker + Docker Compose |

## Inicio Rápido

### Prerrequisitos

- Java 21+
- Docker Desktop
- Gradle 8+ (o usar `gradlew`)

### 1. Variables de Entorno

Copia el archivo de ejemplo y configura tus credenciales locales (Stripe, Postgres, MinIO):

```bash
cp .env.example .env
```

### 2. Levantar infraestructura

```bash
docker compose -f docker/docker-compose.yml up -d
```

Esto levanta: PostgreSQL, Redis, MinIO, RabbitMQ, Elasticsearch, MailHog.

### 3. Compilar y ejecutar

```bash
./gradlew bootRun
```

### 4. Ejecutar Pruebas (Tests)

El proyecto utiliza **Testcontainers** para pruebas de integración con dependencias reales. **Es obligatorio tener Docker encendido** antes de ejecutar este comando:

```bash
./gradlew test
```

### 5. Verificar

```bash
# Health check
curl http://localhost:8080/actuator/health

# Swagger UI
open http://localhost:8080/swagger-ui.html

# MailHog (emails de prueba)
open http://localhost:8025
```

> **Nota para Swagger UI**: Para acceder a endpoints protegidos, primero haz un `POST /api/v1/auth/login`, copia el `accessToken` devuelto, haz clic en el botón **"Authorize"** en Swagger y pega el token.

## API Endpoints

### Auth

| Método | Endpoint | Descripción |
|---|---|---|
| POST | `/api/v1/auth/register` | Registrar usuario |
| POST | `/api/v1/auth/login` | Login (JWT) |
| POST | `/api/v1/auth/refresh` | Refresh token |
| GET | `/api/v1/auth/me` | Perfil actual |

### Tenants (Tiendas)

| Método | Endpoint | Descripción |
|---|---|---|
| POST | `/api/v1/tenants` | Registrar tienda |
| GET | `/api/v1/tenants/{slug}` | Ver tienda (público) |
| GET | `/api/v1/tenants/me` | Mi tienda |

### Catalog (Productos)

| Método | Endpoint | Descripción |
|---|---|---|
| POST | `/api/v1/catalog/categories` | Crear categoría |
| GET | `/api/v1/catalog/categories` | Listar categorías |
| POST | `/api/v1/catalog/products` | Crear producto |
| GET | `/api/v1/catalog/products` | Listar productos |
| GET | `/api/v1/catalog/products/{slug}` | Ver producto |

### Cart (Carrito)

| Método | Endpoint | Descripción |
|---|---|---|
| GET | `/api/v1/cart` | Ver carrito |
| POST | `/api/v1/cart/items` | Agregar item |
| PUT | `/api/v1/cart/items/{id}` | Actualizar cantidad |
| DELETE | `/api/v1/cart/items/{id}` | Remover item |
| DELETE | `/api/v1/cart` | Vaciar carrito |

### Orders (Órdenes)

| Método | Endpoint | Descripción |
|---|---|---|
| GET | `/api/v1/orders` | Listar órdenes |
| GET | `/api/v1/orders/{id}` | Ver orden |
| POST | `/api/v1/orders/{id}/cancel` | Cancelar orden |

### Payments (Pagos)

| Método | Endpoint | Descripción |
|---|---|---|
| POST | `/api/v1/payments/checkout/{orderId}` | Crear checkout |
| GET | `/api/v1/payments/{id}` | Ver pago |
| POST | `/api/v1/payments/webhook/stripe` | Webhook Stripe |

### Search (Búsqueda)

| Método | Endpoint | Descripción |
|---|---|---|
| GET | `/api/v1/search?q=&category=&minPrice=&maxPrice=&sort=` | Buscar productos |
| POST | `/api/v1/search/reindex` | Reindexar (admin) |

## Planes de Suscripción

| Plan | Precio | Productos | Comisión | Features |
|---|---|---|---|---|
| **Free** | $0 | 10 | 5% | Básico |
| **Professional** | $29.99/mes | 1,000 | 2% | Dominio propio, analytics, API |
| **Enterprise** | $99.99/mes | 100,000 | 0.5% | Todo + soporte prioritario, reportes |

## Estructura del Proyecto

```
src/main/java/com/ecommerce/
├── bootstrap/                    # Main, configuración global
└── modules/
    ├── shared/                   # Shared Kernel (DDD)
    │   ├── domain/              # Value Objects, Exceptions, DomainEvent
    │   └── infrastructure/      # JPA config, Security, TenantFilter, Redis
    ├── identity/                 # Auth, Users, JWT
    ├── tenant/                   # Multi-tenancy, Planes, Suscripción
    ├── catalog/                  # Productos, Categorías, Variantes, Imágenes (MinIO)
    ├── cart/                     # Carrito (Redis)
    ├── order/                    # Órdenes con State Machine
    ├── payment/                  # Stripe (checkout + webhooks)
    ├── review/                   # Reseñas y calificaciones
    ├── notification/             # Emails (RabbitMQ consumers)
    └── search/                   # Elasticsearch full-text search
```

## Decisiones de Arquitectura (ADRs)

- [ADR-001: Spring Boot como framework backend](docs/architecture/ADR-001-backend-framework.md)
- [ADR-002: Multi-tenancy con columna discriminadora](docs/architecture/ADR-002-multitenancy-strategy.md)
- [ADR-003: Monolito modular con Clean Architecture](docs/architecture/ADR-003-modular-monolith.md)
- [ADR-004: Autenticación JWT stateless](docs/architecture/ADR-004-jwt-auth.md)

## Licencia

MIT © 2026
