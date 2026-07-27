package com.ecommerce.modulos.analiticas.application.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class ResumenDashboard {
    private BigDecimal ventasTotalesMes;
    private int ordenesTotalesMes;
    private BigDecimal ticketPromedio;
    private List<VentaDiaria> ingresosUltimos7Dias;
    private List<ProductoTop> productosMasVendidos;
}
