package com.ecommerce.modules.tenant.domain;

import com.ecommerce.modules.shared.infrastructure.BaseJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubscriptionPlanRepository extends BaseJpaRepository<SubscriptionPlan> {

    Optional<SubscriptionPlan> findByTipoPlanAndActiveTrue(SubscriptionPlanType tipoPlan);
}
