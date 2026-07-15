package com.ecommerce.modulos.inquilino.domain;

import com.ecommerce.modulos.compartido.infrastructure.RepositorioJpaBase;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RepositorioSuscripcion extends RepositorioJpaBase<Suscripcion> {

    Optional<Suscripcion> findByIdTiendaAndEstado(UUID idTienda, String estado);
}
