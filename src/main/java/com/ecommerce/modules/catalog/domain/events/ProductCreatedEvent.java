package com.ecommerce.modules.catalog.domain.events;

import com.ecommerce.modules.shared.domain.DomainEvent;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class ProductCreatedEvent implements DomainEvent {

    private final UUID eventId;
    private final Instant occurredAt;
    private final UUID productId;
    private final UUID tenantId;
    private final String name;
    private final String slug;
    private final String description;
    private final String status;

    public ProductCreatedEvent(UUID productId, UUID tenantId, String name, String slug, String description, String status) {
        this.eventId = UUID.randomUUID();
        this.occurredAt = Instant.now();
        this.productId = productId;
        this.tenantId = tenantId;
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.status = status;
    }

    @Override
    public String getEventType() {
        return "PRODUCT_CREATED";
    }
}
