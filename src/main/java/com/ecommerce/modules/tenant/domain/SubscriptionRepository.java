package com.ecommerce.modules.tenant.domain;

import com.ecommerce.modules.shared.infrastructure.BaseJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends BaseJpaRepository<Subscription> {

    Optional<Subscription> findByTenantIdAndStatus(UUID tenantId, String status);
}
