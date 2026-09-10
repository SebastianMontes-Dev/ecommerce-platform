package com.ecommerce.modulos.compartido.infrastructure.outbox;

/**
 * Un módulo implementa esta interfaz para procesar los tipos de evento de outbox que le
 * pertenecen (p. ej. {@code busqueda} maneja la indexación en Elasticsearch). Si
 * {@link #procesar} lanza, {@link ProcesadorOutbox} reprograma el evento con backoff.
 */
public interface ManejadorEventoOutbox {

    boolean soporta(String tipo);

    void procesar(EventoOutbox evento) throws Exception;
}
