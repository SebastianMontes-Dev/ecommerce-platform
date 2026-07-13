package com.ecommerce.modules.cart.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonIgnoreProperties(ignoreUnknown = true)
public class CartItem {

    private UUID idProducto;
    private String productName;
    private UUID variantId;
    private String variantName;
    private int cantidad;
    private BigDecimal unitPrice;
    private String currency;
    private String urlImagen;
    private UUID idTienda;

    public BigDecimal getSubtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(cantidad));
    }
}
