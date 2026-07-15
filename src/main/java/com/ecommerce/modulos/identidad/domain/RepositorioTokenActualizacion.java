package com.ecommerce.modulos.identidad.domain;

import com.ecommerce.modulos.compartido.infrastructure.RepositorioJpaBase;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RepositorioTokenActualizacion extends RepositorioJpaBase<TokenActualizacion> {

    Optional<TokenActualizacion> findByToken(String token);

    List<TokenActualizacion> findAllByUserIdAndRevokedFalse(UUID userId);
}
