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
    private String nombre;

    @NotBlank
    private String slug;

    private String descripcion;

    @NotNull
    @Positive
    private BigDecimal precio;

    @Default
    private String currency = "USD";

    private BigDecimal precioComparacion;

    private BigDecimal precioCosto;

    private String sku;

    private String codigoBarras;

    @Positive
    @Default
    private int inventario = 0;

    @Default
    private boolean rastreoInventarioHabilitado = true;

    private java.util.UUID idCategoria;
}
