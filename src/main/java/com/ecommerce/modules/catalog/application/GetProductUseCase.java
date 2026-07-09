package com.ecommerce.modules.catalog.application;

import com.ecommerce.modules.catalog.application.dto.ProductResponse;
import com.ecommerce.modules.catalog.domain.*;
import com.ecommerce.modules.shared.domain.EntityNotFoundException;
import com.ecommerce.modules.shared.infrastructure.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetProductUseCase {

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public ProductResponse bySlug(String slug, UUID tenantId) {
        Product product = productRepository.findByTenantIdAndSlugWithImages(tenantId, slug)
                .orElseThrow(() -> new EntityNotFoundException("Product", slug));
        return CreateProductUseCase.mapToResponse(product);
    }

    @Transactional(readOnly = true)
    public ProductResponse byId(UUID id, UUID tenantId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product", id));

        if (!product.getTenantId().equals(tenantId)) {
            throw new EntityNotFoundException("Product", id);
        }

        return CreateProductUseCase.mapToResponse(product);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> listProducts(UUID tenantId, Pageable pageable) {
        Page<Product> page = productRepository.findAllByTenantId(tenantId, pageable);
        return PagedResponse.from(page.map(CreateProductUseCase::mapToResponse));
    }
}
