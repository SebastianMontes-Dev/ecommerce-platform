package com.ecommerce.modules.catalog.application;

import com.ecommerce.modules.catalog.application.dto.*;
import com.ecommerce.modules.catalog.domain.*;
import com.ecommerce.modules.shared.domain.BusinessRuleViolationException;
import com.ecommerce.modules.shared.domain.EntityNotFoundException;
import com.ecommerce.modules.shared.domain.Money;
import com.ecommerce.modules.shared.infrastructure.TenantContext;
import com.ecommerce.modules.tenant.domain.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateProductUseCase {

    private final ProductRepository productRepository;

    @Transactional
    public ProductResponse execute(CreateProductRequest request, UUID tenantId) {
        if (productRepository.countByTenantId(tenantId) >= 999999) {
            throw new BusinessRuleViolationException("Maximum number of products reached for your plan");
        }

        Product product = new Product();
        product.setTenantId(tenantId);
        product.setName(request.getName());
        product.setSlug(request.getSlug());
        product.setDescription(request.getDescription());
        product.setPrice(Money.of(request.getPrice(), request.getCurrency()));

        if (request.getCompareAtPrice() != null) {
            product.setCompareAtPrice(Money.of(request.getCompareAtPrice(), request.getCurrency()));
        }
        if (request.getCostPrice() != null) {
            product.setCostPrice(Money.of(request.getCostPrice(), request.getCurrency()));
        }

        product.setSku(request.getSku());
        product.setBarcode(request.getBarcode());
        product.setInventory(request.getInventory());
        product.setInventoryTrackEnabled(request.isInventoryTrackEnabled());
        product.setStatus(ProductStatus.DRAFT);

        if (request.getCategoryId() != null) {
            product.setCategoryId(request.getCategoryId());
        }

        product = productRepository.save(product);
        return mapToResponse(product);
    }

    static ProductResponse mapToResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .tenantId(product.getTenantId())
                .name(product.getName())
                .slug(product.getSlug())
                .description(product.getDescription())
                .price(product.getPrice() != null ? product.getPrice().getAmount() : null)
                .currency(product.getPrice() != null ? product.getPrice().getCurrency() : "USD")
                .compareAtPrice(product.getCompareAtPrice() != null ? product.getCompareAtPrice().getAmount() : null)
                .costPrice(product.getCostPrice() != null ? product.getCostPrice().getAmount() : null)
                .sku(product.getSku())
                .barcode(product.getBarcode())
                .inventory(product.getInventory())
                .inventoryTrackEnabled(product.isInventoryTrackEnabled())
                .status(product.getStatus().name())
                .categoryId(product.getCategoryId())
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .variants(product.getVariants() != null ? product.getVariants().stream().map(v ->
                        ProductVariantResponse.builder()
                                .id(v.getId())
                                .name(v.getName())
                                .sku(v.getSku())
                                .price(v.getAmount())
                                .currency(v.getCurrency())
                                .inventory(v.getInventory())
                                .attributes(v.getAttributes())
                                .build()
                ).toList() : List.of())
                .images(product.getImages() != null ? product.getImages().stream().map(i ->
                        ProductImageResponse.builder()
                                .id(i.getId())
                                .url(i.getUrl())
                                .altText(i.getAltText())
                                .width(i.getWidth())
                                .height(i.getHeight())
                                .sortOrder(i.getSortOrder())
                                .build()
                ).toList() : List.of())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
