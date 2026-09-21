---
name: auditoria-seguridad-y-calidad
description: >-
  Usar esta skill para auditar la seguridad, calidad estática y protección de datos en ecommerce-platform: detección de secretos con gitleaks, control de acceso JWT/Spring Security, aislamiento multi-tenant estricto, sanitización de inputs y análisis con SpotBugs o Trivy.
---

# Skill: Auditoría de Seguridad y Calidad (NexaSaaS)

Esta skill proporciona las pautas y listas de verificación para garantizar que el código cumpla con los más altos estándares de seguridad y calidad antes de ser integrado.

---

## 1. Lista de Verificación de Seguridad Obligatoria

### A. Prevención de Fugas de Secretos
- [ ] **Ningún secreto en texto plano**: Verificar que contraseñas, tokens JWT, API keys (Stripe, Elasticsearch, etc.) no estén en archivos `.java`, `.yml`, `.json` o scripts de migración.
- [ ] **Uso estricto de variables de entorno**: Emplear `${NOMBRE_VAR}` apuntando a variables definidas en `.env`.
- [ ] **Comprobación con gitleaks**: Si `gitleaks` está configurado en el sistema o CI, ejecutar detección antes de realizar commits.

### B. Aislamiento Multi-Tenant
- [ ] **Filtro de inquilino activo**: En tablas compartidas por inquilinos estándar, verificar que la entidad cuente con el filtro `@Filter(name = "filtroInquilino")`.
- [ ] **Anti-spoofing**: El `inquilinoId` debe provenir **exclusivamente** del token JWT autenticado (o contexto de seguridad), nunca confiar ciegamente en un header `X-Inquilino-ID` enviado por el cliente.
- [ ] **Enrutamiento dinámico**: Para inquilinos premium con base de datos dedicada, validar que `ConfiguracionMultiTenantDB` resuelva correctamente el DataSource.

### C. Autenticación y Autorización (Spring Security + JWT)
- [ ] **Endpoints protegidos**: Todo endpoint bajo `/api/v1/...` debe tener explícitamente configurada su regla de autorización en `SecurityConfig` (`authenticated()`, `hasRole(...)` o `hasAuthority(...)`).
- [ ] **Principio de mínimo privilegio**: Endpoints de administración reservados exclusivamente para roles autorizados (`ROLE_ADMIN`, `ROLE_SUPER_ADMIN`).
- [ ] **Validación de propiedad de recursos**: Validar siempre que el usuario autenticado sea el dueño real del recurso solicitado (ej. verificar que la orden a cancelar pertenezca al usuario).

### D. Validación de Entradas y Protección contra Inyecciones
- [ ] **Validación con Bean Validation**: Todo DTO de entrada debe tener anotaciones de validación (`@NotNull`, `@NotBlank`, `@Size`, `@Min`, `@Max`, `@Pattern`).
- [ ] **Controladores con `@Valid`**: El parámetro `@RequestBody` debe estar anotado con `@Valid`.
- [ ] **Prevención de SQL Injection**: Utilizar métodos derivados de Spring Data JPA o consultas JPQL/HQL parametrizadas (`:parametro`). Nunca concatenar strings en queries SQL nativas.

---

## 2. Análisis Estático y Herramientas de Calidad

### SpotBugs (Detección de Bugs y Malas Prácticas)
Si el plugin está activo en `build.gradle`:
```bash
./gradlew spotbugsMain
```
Revisar alertas sobre:
- Referencias nulas potenciales (`NP_NULL_ON_SOME_PATH`).
- Mutabilidad expuesta de colecciones o fechas.
- Recursos no cerrados (usar siempre `try-with-resources`).

### Trivy / Dependencias
- Revisar dependencias de Gradle en busca de CVEs conocidas antes de incorporar librerías externas.
- No incorporar dependencias innecesarias o desactualizadas.
