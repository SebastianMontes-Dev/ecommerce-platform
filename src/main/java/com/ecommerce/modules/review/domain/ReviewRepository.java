package com.ecommerce.modules.review.domain;

import com.ecommerce.modules.shared.infrastructure.BaseJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends BaseJpaRepository<Review> {

    Page<Review> findAllByProductIdAndActiveTrue(UUID idProducto, Pageable pageable);

    boolean existsByProductIdAndCustomerIdAndOrderId(UUID idProducto, UUID customerId, UUID idOrden);

    Optional<Review> findByProductIdAndCustomerIdAndOrderId(UUID idProducto, UUID customerId, UUID idOrden);
}
