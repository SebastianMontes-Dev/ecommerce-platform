package com.ecommerce.modulos.pagos.domain;

import com.ecommerce.modulos.compartido.infrastructure.RepositorioJpaBase;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RepositorioPago extends RepositorioJpaBase<Pago> {

    Optional<Pago> findByIdOrden(UUID idOrden);

    Optional<Pago> findByIdExterno(String idExterno);
}
