-- La app y Flyway trabajan sobre el schema `public` (el JDBC URL no fija `currentSchema`).
-- V1__init_schema.sql también crea esta extensión; se deja aquí para que exista antes
-- de que Flyway corra, por si alguna herramienta se conecta antes que la app.
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
