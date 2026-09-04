CREATE TABLE webhook_tenants (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    id_tienda UUID NOT NULL REFERENCES inquilinos(id) ON DELETE CASCADE,
    url_destino VARCHAR(2048) NOT NULL,
    evento VARCHAR(100) NOT NULL,
    secret VARCHAR(255) NOT NULL
);

CREATE INDEX idx_webhook_tenants_tienda_evento ON webhook_tenants(id_tienda, evento);
