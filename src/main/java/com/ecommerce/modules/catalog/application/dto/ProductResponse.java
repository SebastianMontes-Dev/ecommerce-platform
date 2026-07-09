package com.ecommerce.modules.catalog.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {

    private UUID id;
    private UUID tenantId;
    private String name;
    private String slug;
    private String description;
    private BigDecimal price;
    private String currency;
    private BigDecimal compareAtPrice;
    private BigDecimal costPrice;
    private String sku;
    private String barcode;
    private int inventory;
    private boolean inventoryTrackEnabled;
    private String status;
    private UUID categoryId;
    private String categoryName;
    private List<ProductVariantResponse> variants;
    private List<ProductImageResponse> images;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
