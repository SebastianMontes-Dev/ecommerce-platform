---
name: migraciones-flyway-y-db
description: >-
  Usar esta skill al diseñar o modificar tablas, relaciones, índices o tipos de datos en la base de datos PostgreSQL de ecommerce-platform a través de migraciones versionadas de Flyway.
---

# Skill: Migraciones con Flyway y Esquema de Base de Datos (NexaSaaS)

Esta skill establece las reglas y convenciones para evolucionar el esquema de base de datos PostgreSQL de forma segura, determinista y respetando el modelo multi-tenant.

---

## 1. Reglas Fundamentales de Flyway

1. **Flyway es la única fuente de verdad**: Aunque el perfil de desarrollo tenga `spring.jpa.hibernate.ddl-auto: update`, las migraciones SQL en `src/main/resources/db/migration/` determinan el esquema real.
2. **Entorno de pruebas (`application-test.yml`)**: Configurado con `ddl-auto: validate`. Cualquier discrepancia entre las entidades JPA y las tablas generadas por Flyway provocará que los tests fallen al iniciar el contexto.
3. **Inmutabilidad de scripts ya aplicados**: Nunca modificar un script `V1..V8` que ya fue aplicado en local o CI. Si se requiere un cambio, crear una nueva versión consecutiva.

---

## 2. Nomenclatura y Consecutivo de Versiones

- **Formato**: `V{numero}__{descripcion_en_espanol_snake_case}.sql` (doble guión bajo obligatorio después de la versión).
- **Directorio**: `src/main/resources/db/migration/`
- **Versión actual del proyecto**: El esquema actual cuenta con versiones desde `V1` hasta `V8`. Por lo tanto, la próxima migración debe ser:
  `V9__<descripcion_del_cambio>.sql`

---

## 3. Convenciones de Tipos de Datos (PostgreSQL)

| Propósito | Tipo PostgreSQL Recomendado | Notas |
| :--- | :--- | :--- |
| Identificadores primarios | `UUID` | `DEFAULT gen_random_uuid()` |
| Monedas y Precios | `NUMERIC(19, 4)` | Evitar `FLOAT` o `DOUBLE` por pérdida de precisión |
| Fechas y Horas | `TIMESTAMP WITH TIME ZONE` | Guardar siempre en UTC (`timestamptz`) |
| Textos cortos / Enums | `VARCHAR(50)` o `VARCHAR(100)` | No usar enums nativos de Postgres para facilitar portabilidad |
| Textos largos / JSON | `TEXT` o `JSONB` | `JSONB` indexable con GIN para metadata flexible |
| Banderas lógicas | `BOOLEAN` | `DEFAULT TRUE` o `DEFAULT FALSE NOT NULL` |

---

## 4. Requisitos Multi-Tenant en Tablas Compartidas

Toda tabla que pertenezca a un inquilino estándar debe incorporar:
1. Columna de inquilino:
   ```sql
   inquilino_id UUID NOT NULL,
   CONSTRAINT fk_tabla_inquilino FOREIGN KEY (inquilino_id) REFERENCES inquilinos(id) ON DELETE CASCADE
   ```
2. Índice para consultas filtradas (crítico para el rendimiento de `@Filter` de Hibernate):
   ```sql
   CREATE INDEX idx_tabla_inquilino_id ON mi_tabla(inquilino_id);
   ```

---

## 5. Plantilla de Migración

```sql
-- V9__crear_tabla_recompensas.sql

CREATE TABLE IF NOT EXISTS recompensas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    inquilino_id UUID NOT NULL,
    cliente_id UUID NOT NULL,
    puntos INT NOT NULL DEFAULT 0,
    motivo VARCHAR(255) NOT NULL,
    creado_en TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_recompensas_inquilino FOREIGN KEY (inquilino_id) REFERENCES inquilinos(id) ON DELETE CASCADE
);

CREATE INDEX idx_recompensas_inquilino_cliente ON recompensas(inquilino_id, cliente_id);
```
