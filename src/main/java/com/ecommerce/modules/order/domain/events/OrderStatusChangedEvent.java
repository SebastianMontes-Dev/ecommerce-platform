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
    private final UUID orderId;
    private final UUID tenantId;
    private final OrderStatus oldStatus;
    private final OrderStatus newStatus;

    public OrderStatusChangedEvent(UUID orderId, UUID tenantId, OrderStatus oldStatus, OrderStatus newStatus) {
        this.eventId = UUID.randomUUID();
        this.occurredAt = Instant.now();
        this.orderId = orderId;
        this.tenantId = tenantId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
    }

    @Override
    public String getEventType() {
        return "ORDER_STATUS_CHANGED";
    }
}
