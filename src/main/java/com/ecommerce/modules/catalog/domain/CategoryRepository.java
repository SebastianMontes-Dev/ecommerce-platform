package com.ecommerce.modules.catalog.domain;

import com.ecommerce.modules.shared.infrastructure.BaseJpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends BaseJpaRepository<Category> {

    Optional<Category> findByTenantIdAndSlug(UUID tenantId, String slug);

    List<Category> findAllByTenantIdAndParentIdIsNull(UUID tenantId);

    List<Category> findAllByTenantIdAndParentId(UUID tenantId, UUID parentId);

    List<Category> findAllByTenantIdAndActiveTrue(UUID tenantId);

    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.children WHERE c.tenantId = :tenantId AND c.parentId IS NULL")
    List<Category> findRootCategoriesWithChildren(UUID tenantId);
}
