package com.ecommerce.modules.shared.infrastructure;

import com.ecommerce.modules.shared.domain.DomainEvent;
import com.ecommerce.modules.shared.domain.DomainEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SpringDomainEventPublisher implements DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(SpringDomainEventPublisher.class);
    private final ApplicationEventPublisher publisher;

    public SpringDomainEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public <T extends DomainEvent> void publish(T event) {
        log.debug("Publishing domain event: {}", event.getEventType());
        publisher.publishEvent(event);
    }

    @Override
    public <T extends DomainEvent> void publish(List<T> events) {
        events.forEach(this::publish);
    }
}
