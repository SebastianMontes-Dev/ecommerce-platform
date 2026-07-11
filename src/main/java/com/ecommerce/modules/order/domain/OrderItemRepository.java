package com.ecommerce.modules.order.domain;

import com.ecommerce.modules.shared.infrastructure.BaseJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderItemRepository extends BaseJpaRepository<OrderItem> {

    List<OrderItem> findAllByOrderId(UUID idOrden);
}
