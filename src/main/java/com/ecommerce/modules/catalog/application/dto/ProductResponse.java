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
    private UUID idTienda;
    private String nombre;
    private String slug;
    private String descripcion;
    private BigDecimal precio;
    private String currency;
    private BigDecimal precioComparacion;
    private BigDecimal precioCosto;
    private String sku;
    private String codigoBarras;
    private int inventario;
    private boolean rastreoInventarioHabilitado;
    private String estado;
    private UUID idCategoria;
    private String nombreCategoria;
    private List<ProductVariantResponse> variants;
    private List<ProductImageResponse> images;
    private LocalDateTime creadoEn;
    private LocalDateTime actualizadoEn;
}
