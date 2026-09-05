CREATE TABLE envios (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES inquilinos(id) ON DELETE CASCADE,
    id_orden UUID NOT NULL UNIQUE REFERENCES ordenes(id),
    numero_guia VARCHAR(100),
    proveedor VARCHAR(100),
    estado VARCHAR(20) NOT NULL DEFAULT 'PREPARANDO',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by UUID,
    updated_by UUID,
    version BIGINT DEFAULT 0
);

CREATE TABLE evento_tracking (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES inquilinos(id) ON DELETE CASCADE,
    id_envio UUID NOT NULL REFERENCES envios(id) ON DELETE CASCADE,
    estado VARCHAR(20) NOT NULL,
    ubicacion VARCHAR(255),
    descripcion TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by UUID,
    updated_by UUID,
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_envios_tenant ON envios(tenant_id);
CREATE INDEX idx_envios_numero_guia ON envios(numero_guia);
CREATE INDEX idx_evento_tracking_envio ON evento_tracking(id_envio);
