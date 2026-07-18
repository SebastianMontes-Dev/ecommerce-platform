# 🗺️ NexaSaaS Development Roadmap

Este documento detalla el estado actual del proyecto, las deudas técnicas identificadas y el plan de trabajo para completar el desarrollo de la plataforma NexaSaaS.

## 🟢 Fase 1: Correcciones y Arquitectura Limpia (Current Focus)

### 1. Refactorización del Módulo de Reseñas
*   **Problema Actual:** Toda la lógica de negocio y validaciones están dentro de `ControladorResena`. Se ignoran las reglas de negocio críticas de validación de propiedad de orden. Permite usuarios anónimos.
*   **Tareas:**
    *   [ ] Crear `CasoUsoCrearResena` en la capa `application`.
    *   [ ] Implementar verificaciones de negocio:
        *   Validar que el `idOrden` pertenezca al `Usuario` autenticado.
        *   Validar que el estado de la Orden sea `ENTREGADA`.
        *   Validar que la orden contenga el `idProducto` a reseñar.
    *   [ ] Agregar anotaciones de validación (Bean Validation) al DTO `CreateReviewRequest`.
    *   [ ] Eliminar dependencias del Repositorio en el Controlador.

### 2. Refactorización del Módulo de Pagos y Stripe
*   **Problema Actual:** El endpoint `/checkout/{idOrden}` guarda montos en "0" y retorna un link de pago simulado. El Webhook falla al no tener contexto del inquilino (Tenant).
*   **Tareas:**
    *   [ ] Crear `CasoUsoProcesarPago` en `application`.
    *   [ ] Integrar el SDK real de Stripe (`com.stripe.model.checkout.Session`) para generar URLs de pago dinámicas basadas en el total de la orden.
    *   [ ] Modificar la lógica de recepción de Webhooks (`ControladorWebhookStripe`) para poder actualizar el estado del pago independientemente del `Tenant Filter`, posiblemente inyectando lógica a nivel de base de datos o sistema que omita el filtro para eventos asíncronos autorizados.

## 🟡 Fase 2: Características Faltantes

### 3. Sistema de Notificaciones
*   **Estado:** La infraestructura está vacía. Solo existe la definición del dominio.
*   **Tareas:**
    *   [ ] Configurar dependencias de correo (`spring-boot-starter-mail`).
    *   [ ] Implementar `ServicioNotificacionCorreo` conectándolo con un proveedor SMTP (ej. SendGrid o Mailtrap para dev).
    *   [ ] Diseñar plantillas HTML básicas en `templates/email/` para:
        *   Confirmación de Cuenta.
        *   Confirmación de Orden.
        *   Pago Exitoso/Fallido.

## 🔵 Fase 3: Calidad y Pruebas

### 4. Pruebas Automatizadas Backend (Unit & Integration)
*   **Estado:** Solo existe `EcommerceApplicationTests.java` vacío y el script E2E en Python. Faltan pruebas en Java.
*   **Tareas:**
    *   [ ] Configurar base de datos en memoria o Testcontainers para pruebas de integración.
    *   [ ] Escribir pruebas unitarias (JUnit 5 + Mockito) para los Casos de Uso (principalmente Carrito, Ordenes, y Pagos).
    *   [ ] Escribir pruebas de integración (WebMvcTest) para los Controladores y filtros de seguridad (JWT, Tenant Filter).

## 🟣 Fase 4: Deuda Técnica y Refactorizaciones
*   [ ] **Estandarización de Idioma:** Actualmente hay mezcla de español e inglés (`listReviews` vs `resenas`, `amount` vs `moneda`). Migrar gradualmente el código (variables y métodos) a Inglés para cumplir con estándares de la industria, manteniendo el español solo en UI o documentación si es necesario.
*   [ ] **Revisión de Endpoints:** Consolidar el diseño de API REST para asegurar consistencia en las respuestas y en la paginación.
