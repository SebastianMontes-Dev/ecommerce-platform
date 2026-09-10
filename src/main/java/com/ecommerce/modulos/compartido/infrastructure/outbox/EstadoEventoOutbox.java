package com.ecommerce.modulos.compartido.infrastructure.outbox;

public enum EstadoEventoOutbox {
    /** Recién escrito, esperando ser procesado. */
    PENDIENTE,
    /** Reclamado por un worker, en procesamiento. */
    PROCESANDO,
    /** Entregado con éxito al sistema externo. */
    PROCESADO,
    /** Agotó los reintentos; requiere intervención manual. */
    FALLIDO
}
