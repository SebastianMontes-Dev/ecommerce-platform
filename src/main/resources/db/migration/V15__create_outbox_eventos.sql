-- Transactional Outbox: garantiza que un efecto externo (indexar en Elasticsearch, etc.)
-- se registre en la MISMA transacción que el cambio en Postgres. Un worker en segundo
-- plano procesa las filas PENDIENTE con reintentos y backoff, de modo que si el sistema
-- externo está caído, el evento no se pierde.
CREATE TABLE outbox_eventos (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tipo VARCHAR(100) NOT NULL,
    agregado_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    payload JSONB NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    intentos INT NOT NULL DEFAULT 0,
    ultimo_error TEXT,
    reclamado_en TIMESTAMP,
    procesado_en TIMESTAMP,
    proximo_intento_en TIMESTAMP NOT NULL DEFAULT NOW(),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    version BIGINT DEFAULT 0
);

-- El worker consulta filas listas para procesar; el índice parcial las encuentra rápido
-- y se mantiene chico (las PROCESADO/FALLIDO no entran).
CREATE INDEX idx_outbox_listos ON outbox_eventos (proximo_intento_en)
    WHERE estado IN ('PENDIENTE', 'PROCESANDO');
