package com.ecommerce.modules.catalog.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantResponse {

    private UUID id;
    private String nombre;
    private String sku;
    private java.math.BigDecimal precio;
    private String currency;
    private int inventario;
    private Map<String, String> attributes;
}
