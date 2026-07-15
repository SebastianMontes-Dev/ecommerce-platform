package com.ecommerce.modulos.catalogo.application.dto;

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
public class RespuestaVarianteProducto {

    private UUID id;
    private String nombre;
    private String sku;
    private java.math.BigDecimal precio;
    private String moneda;
    private int inventario;
    private Map<String, String> attributes;
}
