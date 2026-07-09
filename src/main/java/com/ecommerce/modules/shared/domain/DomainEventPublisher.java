package com.ecommerce.modules.shared.domain;

import java.util.List;
import java.util.Optional;

public interface DomainEventPublisher {

    <T extends DomainEvent> void publish(T event);

    <T extends DomainEvent> void publish(List<T> events);
}
