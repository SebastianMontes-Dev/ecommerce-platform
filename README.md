# NexaSaaS: Cloud Multi-Tenant E-commerce API ☁️🛒

<div align="center">
  <img src="https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=java" alt="Java 21" />
  <img src="https://img.shields.io/badge/Spring_Boot-3.4.1-brightgreen?style=for-the-badge&logo=spring-boot" alt="Spring Boot" />
  <img src="https://img.shields.io/badge/PostgreSQL-16.0-blue?style=for-the-badge&logo=postgresql" alt="PostgreSQL" />
  <img src="https://img.shields.io/badge/Elasticsearch-8.18-yellow?style=for-the-badge&logo=elasticsearch" alt="Elasticsearch" />
  <img src="https://img.shields.io/badge/Redis-7.2-red?style=for-the-badge&logo=redis" alt="Redis" />
  <img src="https://img.shields.io/badge/Docker-Ready-2496ED?style=for-the-badge&logo=docker" alt="Docker" />
</div>

<br />

> **NexaSaaS** es una API RESTful moderna, escalable y robusta, diseñada para soportar arquitecturas **Multi-Inquilino (Multi-Tenant)** de nivel empresarial (Enterprise SaaS). Permite a múltiples negocios o tiendas virtuales operar bajo la misma infraestructura, manteniendo sus datos completamente aislados, seguros y optimizados para la nube.

---

## 🌟 Características Enterprise (SaaS Avanzado)

- 🏢 **Arquitectura Multi-Inquilino Híbrida**: Aislamiento de datos por `Tenant`. Combina filtros a nivel de fila (`@Filter` de Hibernate) para clientes estándar y **Bases de Datos Dedicadas** (`AbstractRoutingDataSource`) para clientes Premium.
- ⚡ **Búsquedas de Alto Rendimiento (CQRS)**: Sincronización asíncrona de inventario hacia **Elasticsearch** garantizando búsquedas en milisegundos.
- 🤖 **Asistente Virtual de IA**: Endpoint integrado con OpenAI (ChatGPT) para actuar como vendedor virtual en cada tienda.
- 📦 **Motor de Logística y Envíos**: Tracking de paquetes, estados de envío y proveedores integrados a las órdenes.
- 🛍️ **Lógica Comercial Potente**: Cupones de Descuento, Variantes de Producto (tallas, colores), Bloqueo Pesimista (Anti-Sobreventas).
- 📊 **Reportes y Observabilidad**: Reportes en Excel (Apache POI), Dashboards analíticos, Trazabilidad con Zipkin, y Métricas con Prometheus/Grafana.
- 🛡️ **Seguridad Anti-DDoS y Resiliencia**: Cortacircuitos (`Resilience4j`) y Rate Limiting mediante Bucket4j. 
- 💳 **Pagos, Facturas y Reembolsos**: Integración oficial con **Stripe** para pagos y reembolsos, junto con Facturación Electrónica automática en PDF.
- 🔐 **Autenticación Completa**: Login Social (OAuth2 con Google), JWT, control de accesos basados en roles (RBAC).
- ⚡ **Tiempo Real y Eventos**: WebSockets (STOMP) para notificaciones en vivo y un motor de **Outbound Webhooks** con cifrado HMAC-SHA256 para conectarse a plataformas B2B.
- 🕸️ **Capa API Dual**: Endpoints REST tradicionales combinados con una potente capa **GraphQL** nativa para optimizar las cargas de aplicaciones móviles.
- ☁️ **Almacenamiento Cloud**: Subida de imágenes de productos y archivos hacia buckets en MinIO / AWS S3.

---

## 🛠️ Stack Tecnológico

| Capa | Tecnología |
| :--- | :--- |
| **Lenguaje Core** | Java 21 |
| **Framework Base** | Spring Boot 3.4.1 (Web, Security, Data JPA, Actuator) |
| **Bases de Datos** | PostgreSQL 16 (Escritura) / Elasticsearch 8.18 (Lectura) |
| **Caché & Sesiones**| Redis 7.2 |
| **Tolerancia a Fallos**| Resilience4j (Circuit Breakers) |
| **Migraciones** | Flyway |
| **Pruebas (Tests)** | JUnit 5, Mockito, Testcontainers |
| **Infraestructura** | Docker & Docker Compose |

---

## 📂 Arquitectura de Dominio (DDD)

El proyecto sigue un enfoque modular, dividiendo el negocio en micro-módulos dentro de un monolito limpio:

- 🛒 `carrito/` - Interacciones ultrarrápidas con Redis para carritos activos.
- 📦 `catalogo/` - Gestión de productos y variantes (PostgreSQL).
- 🔍 `busqueda/` - Búsqueda indexada de texto completo (Elasticsearch).
- 🏢 `inquilino/` - Gestión de clientes B2B, planes y enrutamiento dinámico de bases de datos.
- 💳 `pagos/` - Patrón Strategy, webhooks e integraciones con Stripe.
- ✉️ `notificacion/` - Eventos de dominio asíncronos y envío de correos.

---

## 🚀 Despliegue Local Rápido

1. **Clonar repositorio**:
   ```bash
   git clone https://github.com/SebastianMontes-Dev/ecommerce-platform.git
   cd ecommerce-platform
   ```

2. **Levantar Infraestructura Base** (Postgres, Redis, Elasticsearch):
   ```bash
   docker-compose up -d
   ```

3. **Compilar y Ejecutar Pruebas (Opcional)**:
   ```bash
   ./gradlew build
   ```

4. **Iniciar la Aplicación Spring Boot**:
   ```bash
   ./gradlew bootRun
   ```

5. **Ver Documentación Interactiva (Swagger UI)**:
   Navega a [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html) para explorar e invocar la API.

---

## 🌐 Visita Nuestra Página Oficial
Visita la documentación oficial y la página de presentación del proyecto (GitHub Pages) configurada en la carpeta `/docs`.

<br/>

<div align="center">
  <i>Desarrollado con pasión, principios SOLID y Clean Code.</i>
</div>
