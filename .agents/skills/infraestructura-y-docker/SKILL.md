---
name: infraestructura-y-docker
description: >-
  Usar esta skill para gestionar, verificar y diagnosticar los contenedores de Docker Compose (Postgres en 5433, Redis, Elasticsearch, RabbitMQ, Zipkin, Mailhog, Prometheus) y la observabilidad de ecommerce-platform.
---

# Skill: Infraestructura y Docker (NexaSaaS)

Esta skill define cómo levantar, verificar y diagnosticar los servicios de soporte en desarrollo local para **NexaSaaS**.

---

## 1. Mapeo de Puertos y Servicios Locales

El archivo `docker/docker-compose.yml` orquesta los siguientes servicios:

| Servicio | Puerto Host | Puerto Contenedor | Propósito |
| :--- | :--- | :--- | :--- |
| **PostgreSQL** | **`5433`** | `5432` | Base de datos principal (`ecommerce_db`) |
| **Redis** | `6379` | `6379` | Caché, rate limiting y sesiones de carrito |
| **Elasticsearch** | `9200` | `9200` | Motor de búsqueda y CQRS de catálogo |
| **RabbitMQ** | `5672`, `15672` | `5672`, `15672` | Mensajería e interfaz de administración |
| **Mailhog** | `1025`, `8025` | `1025`, `8025` | Servidor SMTP mock e interfaz web de correos |
| **Zipkin** | `9411` | `9411` | Trazabilidad distribuida |
| **Prometheus** | `9090` | `9090` | Recolección de métricas de Spring Actuator |
| **Grafana** | `3000` | `3000` | Dashboards de monitoreo |

> [!WARNING]
> Recuerda que el puerto de PostgreSQL en desarrollo es **5433** para evitar colisiones con instancias nativas de Postgres en el puerto estándar 5432.

---

## 2. Comandos Operativos de Docker

```bash
# Levantar toda la infraestructura en segundo plano
docker-compose -f docker/docker-compose.yml up -d

# Verificar estado de los contenedores
docker-compose -f docker/docker-compose.yml ps

# Ver logs de un servicio específico (ej. PostgreSQL o Elasticsearch)
docker-compose -f docker/docker-compose.yml logs -f postgres
docker-compose -f docker/docker-compose.yml logs -f elasticsearch

# Detener los contenedores sin borrar volúmenes
docker-compose -f docker/docker-compose.yml stop

# Detener y remover contenedores
docker-compose -f docker/docker-compose.yml down
```

---

## 3. Diagnóstico de Salud y Observabilidad

Una vez levantada la aplicación Spring Boot en `:8081`:

- **Health Check**: `http://localhost:8081/actuator/health` (verifica conexión a Postgres, Redis y Elasticsearch).
- **Métricas Prometheus**: `http://localhost:8081/actuator/prometheus`.
- **Bandeja de Correos (Mailhog)**: `http://localhost:8025/` para verificar emails de órdenes o notificaciones sin enviar correos reales.
- **Salud de Elasticsearch**: `curl http://localhost:9200/_cluster/health`.
