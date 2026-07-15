CREATE TABLE addresses (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    street VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100),
    codigo_postal VARCHAR(20),
    country VARCHAR(100) NOT NULL,
    additional_info VARCHAR(255),
    is_default BOOLEAN NOT NULL DEFAULT false,
    address_type VARCHAR(20) NOT NULL DEFAULT 'SHIPPING',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    version BIGINT DEFAULT 0
);

CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    numero_orden VARCHAR(100) NOT NULL UNIQUE,
    id_cliente UUID NOT NULL REFERENCES users(id),
    correo_cliente VARCHAR(255) NOT NULL,
    nombre_cliente VARCHAR(255),
    shipping_street VARCHAR(255),
    shipping_city VARCHAR(100),
    shipping_state VARCHAR(100),
    shipping_zip_code VARCHAR(20),
    shipping_country VARCHAR(100),
    shipping_additional_info VARCHAR(255),
    billing_street VARCHAR(255),
    billing_city VARCHAR(100),
    billing_state VARCHAR(100),
    billing_zip_code VARCHAR(20),
    billing_country VARCHAR(100),
    billing_additional_info VARCHAR(255),
    subtotal_amount DECIMAL(10,2) NOT NULL,
    subtotal_currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    monto_impuesto DECIMAL(10,2) NOT NULL DEFAULT 0,
    tax_currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    monto_envio DECIMAL(10,2) NOT NULL DEFAULT 0,
    shipping_currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    total_amount DECIMAL(10,2) NOT NULL,
    total_currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by UUID,
    updated_by UUID,
    version BIGINT DEFAULT 0
);

CREATE TABLE order_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL,
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id UUID NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    variant_id UUID,
    variant_name VARCHAR(255),
    cantidad INTEGER NOT NULL,
    unit_price_amount DECIMAL(10,2) NOT NULL,
    unit_price_currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    subtotal_amount DECIMAL(10,2) NOT NULL,
    subtotal_currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    image_url VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    version BIGINT DEFAULT 0
);

CREATE TABLE order_status_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL,
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    estado_previo VARCHAR(20),
    nuevo_estado VARCHAR(20) NOT NULL,
    changed_by UUID,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_addresses_user ON addresses(user_id);
CREATE INDEX idx_orders_tenant ON orders(tenant_id);
CREATE INDEX idx_orders_customer ON orders(id_cliente);
CREATE INDEX idx_orders_status ON orders(tenant_id, estado);
CREATE INDEX idx_orders_number ON orders(numero_orden);
CREATE INDEX idx_order_items_order ON order_items(order_id);
CREATE INDEX idx_order_history_order ON order_status_history(order_id);
