package com.ecommerce.modules.review.domain;

import com.ecommerce.modules.shared.infrastructure.BaseJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends BaseJpaRepository<Review> {

    Page<Review> findAllByProductIdAndActiveTrue(UUID productId, Pageable pageable);

    boolean existsByProductIdAndCustomerIdAndOrderId(UUID productId, UUID customerId, UUID orderId);

    Optional<Review> findByProductIdAndCustomerIdAndOrderId(UUID productId, UUID customerId, UUID orderId);
}
