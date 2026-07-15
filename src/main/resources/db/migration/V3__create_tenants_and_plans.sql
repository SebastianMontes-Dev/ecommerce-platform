CREATE TABLE inquilinos (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    nombre VARCHAR(255) NOT NULL,
    enlace_corto VARCHAR(100) NOT NULL UNIQUE,
    descripcion TEXT,
    url_logo VARCHAR(500),
    texto_alternativo_logo VARCHAR(255),
    url_banner VARCHAR(500),
    texto_alternativo_banner VARCHAR(255),
    estado VARCHAR(20) NOT NULL DEFAULT 'TRIAL',
    id_propietario UUID NOT NULL REFERENCES usuarios(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by UUID,
    updated_by UUID,
    version BIGINT DEFAULT 0
);

CREATE TABLE subscription_plans (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    nombre VARCHAR(100) NOT NULL,
    tipo_plan VARCHAR(50) NOT NULL,
    precio DECIMAL(10,2) NOT NULL DEFAULT 0,
    max_products INTEGER NOT NULL DEFAULT 10,
    tasa_comision DECIMAL(5,2) NOT NULL DEFAULT 5.00,
    features JSONB NOT NULL DEFAULT '{}',
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    version BIGINT DEFAULT 0
);

CREATE TABLE subscriptions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES inquilinos(id) ON DELETE CASCADE,
    id_plan UUID NOT NULL REFERENCES subscription_plans(id),
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    fecha_inicio TIMESTAMP NOT NULL,
    fecha_fin TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by UUID,
    updated_by UUID,
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_tenants_slug ON inquilinos(enlace_corto);
CREATE INDEX idx_tenants_owner_id ON inquilinos(id_propietario);
CREATE INDEX idx_tenants_status ON inquilinos(estado);
CREATE INDEX idx_subscriptions_tenant_id ON subscriptions(tenant_id);
CREATE INDEX idx_subscription_plans_type ON subscription_plans(tipo_plan);
