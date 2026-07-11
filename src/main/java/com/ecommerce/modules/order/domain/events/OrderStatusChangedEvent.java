package com.ecommerce.modules.order.domain.events;

import com.ecommerce.modules.order.domain.OrderStatus;
import com.ecommerce.modules.shared.domain.DomainEvent;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class OrderStatusChangedEvent implements DomainEvent {

    private final UUID eventId;
    private final Instant occurredAt;
    private final UUID idOrden;
    private final UUID idTienda;
    private final OrderStatus oldStatus;
    private final OrderStatus newStatus;

    public OrderStatusChangedEvent(UUID idOrden, UUID idTienda, OrderStatus oldStatus, OrderStatus newStatus) {
        this.eventId = UUID.randomUUID();
        this.occurredAt = Instant.now();
        this.idOrden = idOrden;
        this.idTienda = idTienda;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
    }

    @Override
    public String getEventType() {
        return "ORDER_STATUS_CHANGED";
    }
}
