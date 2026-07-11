CREATE TABLE categories (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    nombre VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL,
    descripcion TEXT,
    image_url VARCHAR(500),
    parent_id UUID REFERENCES categories(id),
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by UUID,
    updated_by UUID,
    version BIGINT DEFAULT 0,
    UNIQUE(tenant_id, slug)
);

CREATE TABLE products (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    nombre VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL,
    descripcion TEXT,
    amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    compare_at_amount DECIMAL(10,2),
    compare_at_currency VARCHAR(3),
    cost_amount DECIMAL(10,2),
    cost_currency VARCHAR(3),
    sku VARCHAR(100),
    barcode VARCHAR(100),
    inventario INTEGER NOT NULL DEFAULT 0,
    inventory_track_enabled BOOLEAN NOT NULL DEFAULT true,
    allow_backorder BOOLEAN NOT NULL DEFAULT false,
    estado VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    category_id UUID REFERENCES categories(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by UUID,
    updated_by UUID,
    version BIGINT DEFAULT 0,
    UNIQUE(tenant_id, slug)
);

CREATE TABLE product_variants (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    nombre VARCHAR(255) NOT NULL,
    sku VARCHAR(100),
    amount DECIMAL(10,2),
    currency VARCHAR(3),
    inventario INTEGER NOT NULL DEFAULT 0,
    attributes JSONB NOT NULL DEFAULT '{}',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    version BIGINT DEFAULT 0
);

CREATE TABLE product_images (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    url VARCHAR(500) NOT NULL,
    alt_text VARCHAR(255),
    width INTEGER,
    height INTEGER,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_categories_tenant ON categories(tenant_id);
CREATE INDEX idx_categories_parent ON categories(parent_id);
CREATE INDEX idx_products_tenant ON products(tenant_id);
CREATE INDEX idx_products_slug ON products(tenant_id, slug);
CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_products_status ON products(tenant_id, estado);
CREATE INDEX idx_variants_product ON product_variants(product_id);
CREATE INDEX idx_product_images_product ON product_images(product_id);
