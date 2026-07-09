package com.ecommerce.modules.cart.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {

    private UUID productId;
    private String productName;
    private UUID variantId;
    private String variantName;
    private int quantity;
    private BigDecimal unitPrice;
    private String currency;
    private String imageUrl;
    private UUID tenantId;

    public BigDecimal getSubtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
