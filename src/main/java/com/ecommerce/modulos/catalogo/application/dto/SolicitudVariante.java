package com.ecommerce.modulos.catalogo.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudVariante {

    @NotBlank
    private String nombre; // Ej: "Rojo - Talla M"

    private String sku;

    private BigDecimal monto; // Puede ser nulo, en cuyo caso hereda el del producto base

    @Default
    private String moneda = "USD";

    @Positive
    @Default
    private int inventario = 0;

    private Map<String, String> attributes; // Ej: {"Color": "Rojo", "Talla": "M"}
}
