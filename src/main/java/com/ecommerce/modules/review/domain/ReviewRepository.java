package com.ecommerce.modules.review.domain;

import com.ecommerce.modules.shared.infrastructure.BaseJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends BaseJpaRepository<Review> {

    Page<Review> findAllByIdProductoAndActivoTrue(UUID idProducto, Pageable pageable);

    boolean existsByIdProductoAndIdClienteAndIdOrden(UUID idProducto, UUID idCliente, UUID idOrden);

    Optional<Review> findByIdProductoAndIdClienteAndIdOrden(UUID idProducto, UUID idCliente, UUID idOrden);
}
