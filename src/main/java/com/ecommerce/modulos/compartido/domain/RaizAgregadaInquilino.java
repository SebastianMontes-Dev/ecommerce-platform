package com.ecommerce.modulos.compartido.domain;

import jakarta.persistence.MappedSuperclass;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@MappedSuperclass
public abstract class RaizAgregadaInquilino extends EntidadInquilino {

    private transient final List<EventoDominio> domainEvents = new ArrayList<>();

    protected void registerEvent(EventoDominio evento) {
        domainEvents.add(evento);
    }

    public List<EventoDominio> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    public void clearDomainEvents() {
        domainEvents.clear();
    }
}
