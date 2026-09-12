ALTER TABLE webhook_tenants RENAME COLUMN id_tienda TO tenant_id;
ALTER TABLE webhook_tenants
    ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    ADD COLUMN created_by UUID,
    ADD COLUMN updated_by UUID,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- El indice existente ya cubre (tenant_id, evento) tras el rename; Postgres actualiza
-- automaticamente el nombre de columna dentro de la definicion del indice.
