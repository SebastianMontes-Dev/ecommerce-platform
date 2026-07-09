package com.ecommerce.modules.catalog.domain;

import com.ecommerce.modules.shared.infrastructure.BaseJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductVariantRepository extends BaseJpaRepository<ProductVariant> {

    List<ProductVariant> findAllByProductId(UUID productId);
}
