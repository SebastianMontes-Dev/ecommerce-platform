package com.ecommerce.modules.payment.domain;

import com.ecommerce.modules.shared.infrastructure.BaseJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends BaseJpaRepository<Payment> {

    Optional<Payment> findByOrderId(UUID orderId);

    Optional<Payment> findByExternalId(String externalId);
}
