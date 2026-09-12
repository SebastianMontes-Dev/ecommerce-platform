package com.ecommerce.modulos.catalogo.application.dto;

import com.ecommerce.modulos.catalogo.domain.EstadoProducto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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
public class SolicitudCrearProducto {

    @NotBlank
    @Size(max = 200, message = "El nombre no puede superar los 200 caracteres")
    private String nombre;

    @NotBlank
    @Size(max = 200, message = "El enlace corto no puede superar los 200 caracteres")
    private String enlaceCorto;

    @Size(max = 5000, message = "La descripción no puede superar los 5000 caracteres")
    private String descripcion;

    @NotNull
    @Positive
    private BigDecimal precio;

    @Default
    private String moneda = "USD";

    private BigDecimal precioComparacion;

    private BigDecimal precioCosto;

    @Size(max = 100, message = "El SKU no puede superar los 100 caracteres")
    private String sku;

    @Size(max = 100, message = "El código de barras no puede superar los 100 caracteres")
    private String codigoBarras;

    @Positive
    @Default
    private int inventario = 0;

    @Default
    private boolean rastreoInventarioHabilitado = true;

    private java.util.UUID idCategoria;

    private java.util.List<SolicitudVariante> variants;
}
