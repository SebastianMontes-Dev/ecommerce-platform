CREATE TABLE categorias (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES inquilinos(id) ON DELETE CASCADE,
    nombre VARCHAR(255) NOT NULL,
    enlace_corto VARCHAR(255) NOT NULL,
    descripcion TEXT,
    imagen_url VARCHAR(500),
    id_padre UUID REFERENCES categorias(id),
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by UUID,
    updated_by UUID,
    version BIGINT DEFAULT 0,
    UNIQUE(tenant_id, enlace_corto)
);

CREATE TABLE productos (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES inquilinos(id) ON DELETE CASCADE,
    nombre VARCHAR(255) NOT NULL,
    enlace_corto VARCHAR(255) NOT NULL,
    descripcion TEXT,
    amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    monto_comparacion DECIMAL(10,2),
    moneda_comparacion VARCHAR(3),
    monto_costo DECIMAL(10,2),
    moneda_costo VARCHAR(3),
    sku VARCHAR(100),
    codigo_barras VARCHAR(100),
    inventario INTEGER NOT NULL DEFAULT 0,
    rastreo_inventario_habilitado BOOLEAN NOT NULL DEFAULT true,
    permitir_reserva BOOLEAN NOT NULL DEFAULT false,
    estado VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    category_id UUID REFERENCES categorias(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by UUID,
    updated_by UUID,
    version BIGINT DEFAULT 0,
    UNIQUE(tenant_id, enlace_corto)
);

CREATE TABLE product_variants (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL,
    product_id UUID NOT NULL REFERENCES productos(id) ON DELETE CASCADE,
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
    product_id UUID NOT NULL REFERENCES productos(id) ON DELETE CASCADE,
    url VARCHAR(500) NOT NULL,
    alt_text VARCHAR(255),
    width INTEGER,
    height INTEGER,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_categories_tenant ON categorias(tenant_id);
CREATE INDEX idx_categories_parent ON categorias(id_padre);
CREATE INDEX idx_products_tenant ON productos(tenant_id);
CREATE INDEX idx_products_slug ON productos(tenant_id, enlace_corto);
CREATE INDEX idx_products_category ON productos(category_id);
CREATE INDEX idx_products_status ON productos(tenant_id, estado);
CREATE INDEX idx_variants_product ON product_variants(product_id);
CREATE INDEX idx_product_images_product ON product_images(product_id);
