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

    Optional<Product> findByTenantIdAndSlug(UUID idTienda, String slug);

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.images WHERE p.idTienda = :idTienda AND p.slug = :slug")
    Optional<Product> findByTenantIdAndSlugWithImages(UUID idTienda, String slug);

    Page<Product> findAllByTenantId(UUID idTienda, Pageable pageable);

    Page<Product> findAllByTenantIdAndStatus(UUID idTienda, ProductStatus estado, Pageable pageable);

    Page<Product> findAllByTenantIdAndCategoryId(UUID idTienda, UUID idCategoria, Pageable pageable);

    long countByIdTienda(UUID idTienda);

    long countByTenantIdAndStatus(UUID idTienda, ProductStatus estado);
}
