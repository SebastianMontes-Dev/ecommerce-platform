package com.ecommerce.modules.catalog.domain;

import com.ecommerce.modules.shared.infrastructure.BaseJpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends BaseJpaRepository<Category> {

    Optional<Category> findByTenantIdAndSlug(UUID idTienda, String slug);

    List<Category> findAllByTenantIdAndParentIdIsNull(UUID idTienda);

    List<Category> findAllByTenantIdAndParentId(UUID idTienda, UUID parentId);

    List<Category> findAllByTenantIdAndActiveTrue(UUID idTienda);

    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.children WHERE c.idTienda = :idTienda AND c.parentId IS NULL")
    List<Category> findRootCategoriesWithChildren(UUID idTienda);
}
