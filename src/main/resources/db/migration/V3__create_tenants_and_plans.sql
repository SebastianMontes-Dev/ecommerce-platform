CREATE TABLE tenants (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    nombre VARCHAR(255) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    descripcion TEXT,
    logo_url VARCHAR(500),
    logo_alt_text VARCHAR(255),
    banner_url VARCHAR(500),
    banner_alt_text VARCHAR(255),
    estado VARCHAR(20) NOT NULL DEFAULT 'TRIAL',
    owner_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by UUID,
    updated_by UUID,
    version BIGINT DEFAULT 0
);

CREATE TABLE subscription_plans (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    nombre VARCHAR(100) NOT NULL,
    plan_type VARCHAR(50) NOT NULL,
    precio DECIMAL(10,2) NOT NULL DEFAULT 0,
    max_products INTEGER NOT NULL DEFAULT 10,
    commission_rate DECIMAL(5,2) NOT NULL DEFAULT 5.00,
    features JSONB NOT NULL DEFAULT '{}',
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    version BIGINT DEFAULT 0
);

CREATE TABLE subscriptions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    plan_id UUID NOT NULL REFERENCES subscription_plans(id),
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by UUID,
    updated_by UUID,
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_tenants_slug ON tenants(slug);
CREATE INDEX idx_tenants_owner_id ON tenants(owner_id);
CREATE INDEX idx_tenants_status ON tenants(estado);
CREATE INDEX idx_subscriptions_tenant_id ON subscriptions(tenant_id);
CREATE INDEX idx_subscription_plans_type ON subscription_plans(plan_type);
