package com.ecommerce.modulos.analiticas.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.UUID;

@Data
@AllArgsConstructor
public class ProductoTop {
    private UUID idProducto;
    private String nombreProducto;
    private int cantidadVendida;
}
