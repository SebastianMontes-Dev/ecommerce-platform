package com.ecommerce.modules.shared.domain;

import java.time.Instant;
import java.util.UUID;

public interface DomainEvent {

    UUID getIdEvento();

    Instant getOcurrioEn();

    String getTipoEvento();
}
