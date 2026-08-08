# ☁️ NexaSaaS: API de Comercio Electrónico Multi-Inquilino en la Nube 🛒

<div align="center">
  <img src="https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=java&logoColor=white" alt="Java 21" />
  <img src="https://img.shields.io/badge/Spring_Boot-3.4.1-brightgreen?style=for-the-badge&logo=spring-boot&logoColor=white" alt="Spring Boot" />
  <img src="https://img.shields.io/badge/PostgreSQL-16.0-blue?style=for-the-badge&logo=postgresql&logoColor=white" alt="PostgreSQL" />
  <img src="https://img.shields.io/badge/Elasticsearch-8.18-yellow?style=for-the-badge&logo=elasticsearch&logoColor=black" alt="Elasticsearch" />
  <img src="https://img.shields.io/badge/Redis-7.2-red?style=for-the-badge&logo=redis&logoColor=white" alt="Redis" />
  <img src="https://img.shields.io/badge/Docker-Ready-2496ED?style=for-the-badge&logo=docker&logoColor=white" alt="Docker" />
</div>

<br />

> **NexaSaaS** es una API RESTful moderna, escalable y robusta, diseñada para soportar arquitecturas **Multi-Inquilino (Multi-Tenant)** de nivel empresarial (Enterprise SaaS). Permite a múltiples negocios o tiendas virtuales operar bajo la misma infraestructura, manteniendo sus datos completamente aislados, seguros y optimizados para la nube.

---

## 🌟 Características Empresariales (SaaS Avanzado)

- 🏢 **Arquitectura Multi-Inquilino Híbrida**: Aislamiento de datos por `Inquilino`. Combina filtros a nivel de fila (`@Filter` de Hibernate) para clientes estándar y **Bases de Datos Dedicadas** (`AbstractRoutingDataSource`) para clientes Premium.
- ⚡ **Búsquedas de Alto Rendimiento (CQRS)**: Sincronización asíncrona de inventario hacia **Elasticsearch**, garantizando búsquedas en milisegundos.
- 🤖 **Asistente Virtual con IA**: Punto de acceso (Endpoint) integrado con OpenAI (ChatGPT) para actuar como vendedor virtual en cada tienda.
- 📦 **Motor de Logística y Envíos**: Seguimiento de paquetes, estados de envío y proveedores integrados a los pedidos.
- 🛍️ **Lógica Comercial Potente**: Cupones de descuento, variantes de producto (tallas, colores) y bloqueo pesimista (para evitar sobreventas).
- 📊 **Reportes y Observabilidad**: Reportes en Excel (Apache POI), paneles analíticos, trazabilidad con Zipkin, y métricas con Prometheus/Grafana.
- 🛡️ **Seguridad Anti-DDoS y Resiliencia**: Cortacircuitos (`Resilience4j`) y limitación de peticiones (Rate Limiting) mediante Bucket4j.
- 💳 **Pagos, Facturas y Reembolsos**: Integración oficial con **Stripe** para pagos y reembolsos, junto con facturación electrónica automática en formato PDF.
- 🔐 **Autenticación Completa**: Inicio de sesión social (OAuth2 con Google), JWT, y control de accesos basados en roles (RBAC).
- ⚡ **Tiempo Real y Eventos**: WebSockets (STOMP) para notificaciones en vivo y un motor de **Webhooks Salientes (Outbound Webhooks)** con cifrado HMAC-SHA256 para conectarse a plataformas empresariales (B2B).
- 🕸️ **Capa API Dual**: Puntos de acceso REST tradicionales combinados con una potente capa **GraphQL** nativa para optimizar la carga en aplicaciones móviles.
- ☁️ **Almacenamiento en la Nube (Cloud Storage)**: Subida de imágenes de productos y archivos hacia repositorios en MinIO o AWS S3.

---

## 🛠️ Tecnologías Utilizadas

| Capa | Tecnología |
| :--- | :--- |
| **Lenguaje Principal** | Java 21 |
| **Marco de Trabajo (Framework)** | Spring Boot 3.4.1 (Web, Security, Data JPA, Actuator) |
| **Bases de Datos** | PostgreSQL 16 (Escritura) / Elasticsearch 8.18 (Lectura) |
| **Caché y Sesiones**| Redis 7.2 |
| **Tolerancia a Fallos**| Resilience4j (Cortacircuitos) |
| **Migraciones** | Flyway |
| **Pruebas Automatizadas** | JUnit 5, Mockito, Testcontainers |
| **Infraestructura** | Docker y Docker Compose |

---

## 📂 Arquitectura de Dominio (DDD)

El proyecto sigue un enfoque modular, dividiendo la lógica de negocio en micromódulos dentro de un monolito limpio:

- 🛒 `carrito/` - Interacciones ultrarrápidas con Redis para carritos activos.
- 📦 `catalogo/` - Gestión de productos y variantes (PostgreSQL).
- 🔍 `busqueda/` - Búsqueda indexada de texto completo (Elasticsearch).
- 🏢 `inquilino/` - Gestión de clientes corporativos (B2B), planes y enrutamiento dinámico de bases de datos.
- 💳 `pagos/` - Patrón Estrategia (Strategy), webhooks e integraciones con Stripe.
- ✉️ `notificacion/` - Eventos de dominio asíncronos y envío de correos electrónicos.

---

## 🚀 Despliegue Local Rápido

1. **Clonar el repositorio**:
   ```bash
   git clone https://github.com/SebastianMontes-Dev/ecommerce-platform.git
   cd ecommerce-platform
   ```

2. **Levantar la Infraestructura Base** (PostgreSQL, Redis, Elasticsearch):
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

5. **Ver la Documentación Interactiva (Swagger UI)**:
   Navega a [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html) para explorar e invocar la API.

---

## 🌐 Visita Nuestra Página Oficial
Visita la documentación oficial y la página de presentación del proyecto (GitHub Pages) configurada en la carpeta `/docs`.

<br/>

<div align="center">
  <i>Desarrollado con pasión, principios SOLID y Código Limpio (Clean Code).</i>
</div>
