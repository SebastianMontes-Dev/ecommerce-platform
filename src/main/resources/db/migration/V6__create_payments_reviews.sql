CREATE TABLE pagos (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES inquilinos(id) ON DELETE CASCADE,
    order_id UUID NOT NULL REFERENCES ordenes(id),
    amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    metodo_pago VARCHAR(20) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    id_externo VARCHAR(255),
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by UUID,
    updated_by UUID,
    version BIGINT DEFAULT 0
);

CREATE TABLE refunds (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL,
    payment_id UUID NOT NULL REFERENCES pagos(id) ON DELETE CASCADE,
    amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    reason TEXT,
    id_externo VARCHAR(255),
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    version BIGINT DEFAULT 0
);

CREATE TABLE resenas (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES inquilinos(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES productos(id) ON DELETE CASCADE,
    id_cliente UUID NOT NULL REFERENCES usuarios(id),
    order_id UUID NOT NULL REFERENCES ordenes(id),
    value DECIMAL(2,1) NOT NULL CHECK (value >= 0 AND value <= 5),
    title VARCHAR(255),
    comment TEXT,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    version BIGINT DEFAULT 0,
    UNIQUE(product_id, id_cliente, order_id)
);

CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    tipo_notificacion VARCHAR(50) NOT NULL,
    correo_destinatario VARCHAR(255) NOT NULL,
    subject VARCHAR(500),
    body TEXT,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    enviado_en TIMESTAMP,
    mensaje_error TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_payments_order ON pagos(order_id);
CREATE INDEX idx_payments_external ON pagos(id_externo);
CREATE INDEX idx_payments_status ON pagos(estado);
CREATE INDEX idx_reviews_product ON resenas(product_id);
CREATE INDEX idx_reviews_customer ON resenas(id_cliente);
CREATE INDEX idx_reviews_tenant ON resenas(tenant_id);
CREATE INDEX idx_notifications_user ON notifications(user_id);
CREATE INDEX idx_notifications_status ON notifications(estado);
