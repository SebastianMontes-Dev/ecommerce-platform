CREATE TABLE processed_events (
    id_evento VARCHAR(255) PRIMARY KEY,
    tipo_evento VARCHAR(255) NOT NULL,
    procesado_en TIMESTAMP NOT NULL DEFAULT NOW()
);
