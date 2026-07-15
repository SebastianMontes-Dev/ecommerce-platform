package com.ecommerce.modulos.compartido.infrastructure;

import com.ecommerce.modulos.compartido.domain.EventoDominio;
import com.ecommerce.modulos.compartido.domain.PublicadorEventoDominio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PublicadorEventoDominioSpring implements PublicadorEventoDominio {

    private static final Logger log = LoggerFactory.getLogger(PublicadorEventoDominioSpring.class);
    private final ApplicationEventPublisher publisher;

    public PublicadorEventoDominioSpring(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public <T extends EventoDominio> void publish(T evento) {
        log.debug("Publishing domain evento: {}", evento.getTipoEvento());
        publisher.publishEvent(evento);
    }

    @Override
    public <T extends EventoDominio> void publish(List<T> eventos) {
        eventos.forEach(this::publish);
    }
}
