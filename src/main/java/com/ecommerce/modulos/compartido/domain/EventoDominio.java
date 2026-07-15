package com.ecommerce.modulos.compartido.domain;

import java.time.Instant;
import java.util.UUID;

public interface EventoDominio {

    UUID getIdEvento();

    Instant getOcurrioEn();

    String getTipoEvento();
}
