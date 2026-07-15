package com.ecommerce.modulos.ordenes.domain.eventos;

import com.ecommerce.modulos.ordenes.domain.EstadoOrden;
import com.ecommerce.modulos.compartido.domain.EventoDominio;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class EventoEstadoOrdenCambiado implements EventoDominio {

    private final UUID idEvento;
    private final Instant ocurrioEn;
    private final UUID idOrden;
    private final UUID idTienda;
    private final EstadoOrden estadoAnterior;
    private final EstadoOrden nuevoEstado;
    private final String notes;

    public EventoEstadoOrdenCambiado(UUID idOrden, UUID idTienda, EstadoOrden estadoAnterior, EstadoOrden nuevoEstado, String notes) {
        this.idEvento = UUID.randomUUID();
        this.ocurrioEn = Instant.now();
        this.idOrden = idOrden;
        this.idTienda = idTienda;
        this.estadoAnterior = estadoAnterior;
        this.nuevoEstado = nuevoEstado;
        this.notes = notes;
    }

    @Override
    public String getTipoEvento() {
        return "ORDER_STATUS_CHANGED";
    }
}
