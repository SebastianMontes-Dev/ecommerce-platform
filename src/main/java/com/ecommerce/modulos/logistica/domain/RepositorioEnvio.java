package com.ecommerce.modulos.logistica.domain;

import com.ecommerce.modulos.compartido.infrastructure.RepositorioJpaBase;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RepositorioEnvio extends RepositorioJpaBase<Envio> {
    Optional<Envio> findByIdTiendaAndIdOrden(UUID idTienda, UUID idOrden);
    Optional<Envio> findByIdTiendaAndNumeroGuia(UUID idTienda, String numeroGuia);
}
