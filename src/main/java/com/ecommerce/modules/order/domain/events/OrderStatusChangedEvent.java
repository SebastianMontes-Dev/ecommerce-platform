package com.ecommerce.modules.order.domain.events;

import com.ecommerce.modules.order.domain.OrderStatus;
import com.ecommerce.modules.shared.domain.DomainEvent;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class OrderStatusChangedEvent implements DomainEvent {

    private final UUID idEvento;
    private final Instant ocurrioEn;
    private final UUID idOrden;
    private final UUID idTienda;
    private final OrderStatus estadoAnterior;
    private final OrderStatus nuevoEstado;
    private final String notes;

    public OrderStatusChangedEvent(UUID idOrden, UUID idTienda, OrderStatus estadoAnterior, OrderStatus nuevoEstado, String notes) {
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
