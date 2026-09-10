package com.ecommerce.modulos.compartido.infrastructure.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Escribe una fila en el outbox. {@code propagation = MANDATORY}: <b>siempre</b> debe
 * llamarse dentro de una transacción de negocio ya abierta, para que la fila y el cambio
 * que la origina commiteen juntos (o ninguno). Si se llama sin transacción, lanza.
 */
@Component
@RequiredArgsConstructor
public class RegistroOutbox {

    private final RepositorioEventoOutbox repositorio;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public void registrar(String tipo, UUID agregadoId, UUID idTienda, Object payload) {
        repositorio.save(new EventoOutbox(tipo, agregadoId, idTienda, aJson(payload)));
    }

    private String aJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Payload de outbox no serializable: " + payload.getClass(), e);
        }
    }
}
