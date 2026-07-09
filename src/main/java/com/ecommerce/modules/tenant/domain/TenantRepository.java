package com.ecommerce.modules.tenant.domain;

import com.ecommerce.modules.shared.infrastructure.BaseJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantRepository extends BaseJpaRepository<Tenant> {

    Optional<Tenant> findBySlug(String slug);

    Optional<Tenant> findByOwnerId(UUID ownerId);

    boolean existsBySlug(String slug);

    boolean existsByOwnerId(UUID ownerId);
}
