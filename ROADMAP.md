# 🗺️ Hoja de Ruta de NexaSaaS (Roadmap)

<div align="center">
  <img src="https://img.shields.io/badge/Estado-En_Desarrollo-blue?style=for-the-badge" alt="Estado" />
  <img src="https://img.shields.io/badge/Visión-SaaS_Empresarial-purple?style=for-the-badge" alt="Visión" />
</div>

<br />

Esta hoja de ruta detalla la visión y los próximos pasos para la evolución de **NexaSaaS**, organizada en fases a corto, mediano y largo plazo. 🚀

---

## 🟢 Fase 1: Corto Plazo (Fundamentos y Estabilización)

En esta fase, nos centramos en perfeccionar las características actuales, mejorar la seguridad y optimizar el rendimiento de la API.

- [ ] **Optimización del Rendimiento (Performance)**:
  - Mejorar el tiempo de respuesta de la sincronización de inventario con Elasticsearch.
  - Implementar estrategias de caché más agresivas con Redis para catálogos altamente consultados.
- [ ] **Auditoría de Seguridad**:
  - [x] Aislamiento multi-tenant: cerrado el spoofing del header `X-Inquilino-ID` y activado el filtro `@Filter` de Hibernate como defensa de fondo (ver [`docs/superpowers/plans/completados/2026-09-04-aislamiento-multi-tenant.md`](docs/superpowers/plans/completados/2026-09-04-aislamiento-multi-tenant.md)).
  - Pruebas de penetración y revisión de vulnerabilidades en la integración de OAuth2 y JWT.
  - Ajuste fino de reglas en Bucket4j para limitar el abuso de la API.
- [ ] **Mejoras en el Panel de Administración (Backoffice)**:
  - Puntos de acceso adicionales para la gestión visual de Inquilinos y planes de suscripción.
  - Generación de reportes de ventas detallados con Apache POI.
- [ ] **Pruebas y Calidad de Código (QA)**:
  - Aumentar la cobertura de pruebas unitarias y de integración al 85%.
  - Integración continua más robusta utilizando GitHub Actions.

---

## 🟡 Fase 2: Mediano Plazo (Crecimiento y Nuevas Funcionalidades)

El objetivo a mediano plazo es ampliar el ecosistema añadiendo herramientas que aporten más valor a los comercios.

- [ ] **Integraciones Logísticas Adicionales**:
  - Incorporar conectores nativos con DHL, FedEx y otros proveedores logísticos globales.
  - Cálculo de tarifas de envío en tiempo real.
- [ ] **Módulo de Marketing y Fidelización**:
  - Sistema avanzado de puntos y recompensas para clientes.
  - Campañas de correos masivos y notificaciones push segmentadas.
- [ ] **Expansión de la Inteligencia Artificial**:
  - Recomendaciones de productos personalizadas basadas en el historial de navegación y compras utilizando IA.
  - Análisis de sentimientos en reseñas y calificaciones de productos.
- [ ] **Diversificación de Pagos**:
  - Añadir soporte para PayPal, MercadoPago y criptomonedas (Ej: Coinbase Commerce).
- [ ] **Internacionalización (i18n) Completa**:
  - Soporte multi-divisa y actualización de tasas de cambio en tiempo real.
  - Catálogo de productos multilingüe.

---

## 🔴 Fase 3: Largo Plazo (Escala Global y Ecosistema)

A largo plazo, NexaSaaS se transformará en una plataforma autónoma, orientada a microservicios si la escala lo demanda, y abierta a desarrolladores de terceros.

- [ ] **Evolución a Microservicios (Si aplica)**:
  - Extraer dominios pesados (Ej: motor de búsqueda, procesamiento de pagos) a microservicios independientes.
  - Despliegue orquestado con Kubernetes (K8s) y Service Mesh (Istio).
- [ ] **Mercado de Aplicaciones (App Store)**:
  - Creación de una plataforma donde desarrolladores externos puedan crear y publicar extensiones (plugins) para NexaSaaS.
- [ ] **Analítica de Datos Avanzada (Big Data)**:
  - Migración del análisis de datos masivos a herramientas como Apache Kafka y un almacén de datos (Data Warehouse) en la nube (Snowflake o BigQuery).
  - Predicción de demanda y optimización inteligente de inventario.
- [ ] **Comercio sin Cabeza (Headless Commerce) Extendido**:
  - Kits de desarrollo (SDKs) nativos para iOS, Android, y frameworks frontend (React, Vue, Angular).
  - Plantillas de inicio rápido para clientes corporativos (B2B).

---

<div align="center">
  <i>💡 ¿Tienes ideas o sugerencias? ¡Abre un 'Issue' o contribuye al proyecto!</i>
</div>
