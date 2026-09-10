package com.ecommerce.modulos.busqueda.application;

import com.ecommerce.modulos.busqueda.domain.DocumentoProducto;
import com.ecommerce.modulos.compartido.infrastructure.outbox.EventoOutbox;
import com.ecommerce.modulos.compartido.infrastructure.outbox.ManejadorEventoOutbox;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Procesa las filas de outbox de tipo {@code BUSQUEDA_*}: entrega la (des)indexación a
 * Elasticsearch. Si {@link ServicioBusqueda} lanza (ES caído), {@code ProcesadorOutbox}
 * reprograma el evento con backoff.
 */
@Component
@RequiredArgsConstructor
public class ManejadorIndexacionOutbox implements ManejadorEventoOutbox {

    private final ServicioBusqueda servicioBusqueda;
    private final ObjectMapper objectMapper;

    @Override
    public boolean soporta(String tipo) {
        return PuenteOutboxIndexacion.TIPO_INDEXAR.equals(tipo)
                || PuenteOutboxIndexacion.TIPO_ELIMINAR.equals(tipo);
    }

    @Override
    public void procesar(EventoOutbox evento) throws Exception {
        if (PuenteOutboxIndexacion.TIPO_ELIMINAR.equals(evento.getTipo())) {
            servicioBusqueda.deleteProduct(evento.getIdTienda(), evento.getAgregadoId().toString());
        } else {
            DocumentoProducto doc = objectMapper.readValue(evento.getPayload(), DocumentoProducto.class);
            servicioBusqueda.indexProduct(doc);
        }
    }
}
