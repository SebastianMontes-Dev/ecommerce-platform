package com.ecommerce.modulos.compartido.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class RaizAgregada<I> extends EntidadAuditableBase {

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
