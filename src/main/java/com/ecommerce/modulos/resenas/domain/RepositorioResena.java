package com.ecommerce.modulos.resenas.domain;

import com.ecommerce.modulos.compartido.infrastructure.RepositorioJpaBase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RepositorioResena extends RepositorioJpaBase<Resena> {

    Page<Resena> findAllByIdProductoAndActivoTrue(UUID idProducto, Pageable pageable);

    boolean existsByIdProductoAndIdClienteAndIdOrden(UUID idProducto, UUID idCliente, UUID idOrden);

    Optional<Resena> findByIdProductoAndIdClienteAndIdOrden(UUID idProducto, UUID idCliente, UUID idOrden);
}
