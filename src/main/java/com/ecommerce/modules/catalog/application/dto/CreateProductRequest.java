package com.ecommerce.modules.catalog.application.dto;

import com.ecommerce.modules.catalog.domain.ProductStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String slug;

    private String description;

    @NotNull
    @Positive
    private BigDecimal price;

    @Default
    private String currency = "USD";

    private BigDecimal compareAtPrice;

    private BigDecimal costPrice;

    private String sku;

    private String barcode;

    @Positive
    @Default
    private int inventory = 0;

    @Default
    private boolean inventoryTrackEnabled = true;

    private java.util.UUID categoryId;
}
