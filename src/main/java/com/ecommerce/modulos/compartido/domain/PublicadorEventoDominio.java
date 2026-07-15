package com.ecommerce.modulos.compartido.domain;

import java.util.List;
import java.util.Optional;

public interface PublicadorEventoDominio {

    <T extends EventoDominio> void publish(T evento);

    <T extends EventoDominio> void publish(List<T> eventos);
}
