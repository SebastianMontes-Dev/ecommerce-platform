package com.ecommerce.modulos.analiticas.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class VentaDiaria {
    private String fecha;
    private BigDecimal monto;
    private int cantidadOrdenes;
}
