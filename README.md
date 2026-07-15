# NexaSaaS: Cloud Multi-Tenant E-commerce API

![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=java)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.4.1-green?style=flat-square&logo=spring-boot)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16.0-blue?style=flat-square&logo=postgresql)
![Redis](https://img.shields.io/badge/Redis-7.2-red?style=flat-square&logo=redis)
![Docker](https://img.shields.io/badge/Docker-Enabled-blue?style=flat-square&logo=docker)

NexaSaaS es una API RESTful moderna, escalable y robusta, diseñada para soportar arquitecturas **Multi-Inquilino (Multi-Tenant)**. Permite a múltiples negocios o tiendas virtuales operar bajo la misma infraestructura, compartiendo recursos de hardware pero manteniendo sus datos completamente aislados de forma segura mediante filtrado a nivel de base de datos (Data Isolation).

Este proyecto sirve como el núcleo (Backend) de un modelo de negocio de **Software as a Service (SaaS)** para tiendas en línea.

---

## 🚀 Características Principales

*   **Arquitectura Multi-Inquilino Robusta:** Aislamiento de datos implementado mediante filtros de Hibernate (`@Filter`), asegurando que ningún inquilino (tienda) pueda acceder a datos de otro.
*   **Gestión Completa de Identidad y Acceso (IAM):** Registro de usuarios, autenticación basada en JWT, y roles (Propietario de tienda, Cliente, Administrador Global).
*   **Catálogo Avanzado:** Gestión jerárquica de categorías, control de inventario en tiempo real, SKUs y rastreo de stock por variante de producto.
*   **Flujo de Compras (Checkout) Seguro:** Carrito de compras implementado de alto rendimiento con **Redis** y persistencia relacional en PostgreSQL para asegurar durabilidad e integridad en las órdenes de compra.
*   **Sistema de Reseñas y Calificaciones:** Sólo clientes verificados pueden calificar productos tras confirmar la recepción de su orden.
*   **Diseñado para Cloud:** Infraestructura completamente orquestada con Docker y contenedores para rápido despliegue (PostgreSQL, Redis, Servidor).

---

## 🛠️ Stack Tecnológico

*   **Lenguaje:** Java 21
*   **Framework:** Spring Boot 3.4.1 (Spring Web, Spring Data JPA, Spring Security)
*   **Base de Datos Relacional:** PostgreSQL 16 (con Hibernate ORM)
*   **Caché y Almacenamiento en Memoria:** Redis 7.2 (Lettuce, Spring Data Redis)
*   **Migración de Base de Datos:** Flyway
*   **Seguridad:** JSON Web Tokens (JWT)
*   **Documentación de API:** OpenAPI (Swagger UI) / SpringDoc
*   **Orquestación:** Docker y Docker Compose

---

## 📂 Estructura del Proyecto

El código fuente está estructurado siguiendo principios de **Domain-Driven Design (DDD)** adaptado a micro-módulos dentro de un monolito modular.

```text
src/main/java/com/ecommerce/
│
├── modulos/
│   ├── compartido/       # (Core) Interfaces, Entidades Base, Excepciones Globales, Contexto Inquilino
│   ├── identidad/        # (Auth) JWT, Controladores de Registro y Login, Repositorios de Usuario
│   ├── inquilino/        # (Tenant) Administración de Tiendas, Registro de Subdominios
│   ├── catalogo/         # (Catalog) Gestión de Categorías, Productos y Variantes
│   ├── carrito/          # (Cart) Interacciones en Redis para los carritos activos
│   ├── ordenes/          # (Orders) Casos de uso de Checkout, Generación de Órdenes y Totales
│   ├── pagos/            # (Payments) Webhooks e integración con pasarelas (Stripe)
│   └── resenas/          # (Reviews) Lógica para calificación de órdenes finalizadas
│
└── EcommercePlatformApplication.java
```

---

## ⚙️ Requisitos Previos

Para correr este proyecto en tu entorno local necesitas:

*   **Java 21 JDK** o superior.
*   **Docker Desktop** (para levantar PostgreSQL y Redis).
*   **Gradle** (aunque se incluye Gradle Wrapper).

---

## 🏃🏻‍♂️ Cómo Iniciar (Getting Started)

1. **Clonar el repositorio:**
   ```bash
   git clone https://github.com/tu-usuario/nexasaas-ecommerce.git
   cd nexasaas-ecommerce
   ```

2. **Levantar la Infraestructura (Docker):**
   Este comando descargará y levantará contenedores para Postgres (puerto `5433`) y Redis (puerto `6380`).
   ```bash
   docker-compose up -d
   ```
   *Nota: Si estás usando Windows para desarrollar y Docker en WSL, asegúrate de configurar la variable de entorno `DOCKER_HOST` si tu terminal lo requiere, aunque usualmente Docker Desktop gestiona esto en automático.*

3. **Ejecutar la Aplicación Spring Boot:**
   ```bash
   ./gradlew bootRun
   ```

4. **Acceder a la Documentación (Swagger):**
   Una vez que la aplicación ha iniciado con éxito (tarda en promedio ~3 segundos), podrás explorar toda la API y hacer llamadas directas en:
   👉 **`http://localhost:8081/swagger-ui.html`**

---

## 🧪 Pruebas E2E Automatizadas

Para validar que el flujo completo (desde la creación de una cuenta hasta el pago de una orden) funciona correctamente, he desarrollado un **script de pruebas End-to-End (E2E)** ubicado en la carpeta `scripts/`.

Para ejecutarlo (requiere Python 3):
```bash
cd scripts
python prueba_e2e.py
```
*Este script simulará un usuario real registrándose, creando su tienda SaaS, poblando el catálogo, agregando productos a su carrito y finalizando su primera compra, retornando código `HTTP 201 Created` en cada paso exitoso.*

---

## 🛡️ Aspectos de Seguridad y Buenas Prácticas

1. **Aislamiento a Nivel SQL (Row-Level Tenancy):** A cada solicitud HTTP autenticada se le asigna su `X-Inquilino-ID`. Hibernate intercepta y filtra automáticamente todos los SELECTs, UPDATEs y DELETEs sin que el desarrollador tenga que añadir manualmente `WHERE tenant_id=?` a cada método del repositorio.
2. **Encriptación de Contraseñas:** Integrado con `BCryptPasswordEncoder`.
3. **Manejo de Errores Global:** Implementación de `@ControllerAdvice` que formatea excepciones estandarizadas.
4. **Separación de Responsabilidades:** Arquitectura hexagonal / Clean Architecture por capas (Application, Domain, Infrastructure).

---

> Desarrollado con ☕ y buenas prácticas de ingeniería de software.
