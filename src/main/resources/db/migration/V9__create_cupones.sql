CREATE TABLE cupones (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES inquilinos(id) ON DELETE CASCADE,
    codigo VARCHAR(50) NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    valor DECIMAL(10,2) NOT NULL,
    fecha_expiracion TIMESTAMP,
    limite_usos INTEGER,
    usos_actuales INTEGER NOT NULL DEFAULT 0,
    activo BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by UUID,
    updated_by UUID,
    version BIGINT DEFAULT 0,
    UNIQUE(tenant_id, codigo)
);

CREATE INDEX idx_cupones_tenant ON cupones(tenant_id);
CREATE INDEX idx_cupones_codigo ON cupones(codigo);
