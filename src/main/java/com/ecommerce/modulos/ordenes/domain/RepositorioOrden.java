package com.ecommerce.modulos.ordenes.domain;

import com.ecommerce.modulos.compartido.infrastructure.RepositorioJpaBase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RepositorioOrden extends RepositorioJpaBase<Orden> {

    Optional<Orden> findByNumeroOrden(String numeroOrden);

    Page<Orden> findAllByIdTienda(UUID idTienda, Pageable pageable);

    Page<Orden> findAllByIdCliente(UUID idCliente, Pageable pageable);
}
