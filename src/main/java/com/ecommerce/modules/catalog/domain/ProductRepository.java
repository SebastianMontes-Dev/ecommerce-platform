package com.ecommerce.modules.catalog.domain;

import com.ecommerce.modules.shared.infrastructure.BaseJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends BaseJpaRepository<Product> {

    Optional<Product> findByTenantIdAndSlug(UUID tenantId, String slug);

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.images WHERE p.tenantId = :tenantId AND p.slug = :slug")
    Optional<Product> findByTenantIdAndSlugWithImages(UUID tenantId, String slug);

    Page<Product> findAllByTenantId(UUID tenantId, Pageable pageable);

    Page<Product> findAllByTenantIdAndStatus(UUID tenantId, ProductStatus status, Pageable pageable);

    Page<Product> findAllByTenantIdAndCategoryId(UUID tenantId, UUID categoryId, Pageable pageable);

    long countByTenantId(UUID tenantId);

    long countByTenantIdAndStatus(UUID tenantId, ProductStatus status);
}
