package com.ecommerce.modulos.inquilino.domain;

import com.ecommerce.modulos.compartido.infrastructure.RepositorioJpaBase;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RepositorioInquilino extends RepositorioJpaBase<Inquilino> {

    Optional<Inquilino> findByEnlaceCorto(String enlaceCorto);

    Optional<Inquilino> findByIdPropietario(UUID idPropietario);

    boolean existsByEnlaceCorto(String enlaceCorto);

    boolean existsByIdPropietario(UUID idPropietario);
}
