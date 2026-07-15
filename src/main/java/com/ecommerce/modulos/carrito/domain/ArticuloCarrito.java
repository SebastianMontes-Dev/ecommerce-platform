package com.ecommerce.modulos.carrito.domain;

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
public class ArticuloCarrito {

    private UUID idProducto;
    private String nombreProducto;
    private UUID variantId;
    private String variantName;
    private int cantidad;
    private BigDecimal precioUnitario;
    private String moneda;
    private String urlImagen;
    private UUID idTienda;

    public BigDecimal getSubtotal() {
        return precioUnitario.multiply(BigDecimal.valueOf(cantidad));
    }
}
