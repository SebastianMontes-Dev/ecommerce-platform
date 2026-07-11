package com.ecommerce.modules.order.domain;

import com.ecommerce.modules.shared.infrastructure.BaseJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends BaseJpaRepository<Order> {

    Optional<Order> findByOrderNumber(String orderNumber);

    Page<Order> findAllByTenantId(UUID idTienda, Pageable pageable);

    Page<Order> findAllByCustomerId(UUID customerId, Pageable pageable);
}
