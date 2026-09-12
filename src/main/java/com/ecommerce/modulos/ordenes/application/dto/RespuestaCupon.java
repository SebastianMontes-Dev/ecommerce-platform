package com.ecommerce.modulos.ordenes.application.dto;

import com.ecommerce.modulos.ordenes.domain.TipoDescuento;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RespuestaCupon {
    private UUID id;
    private String codigo;
    private TipoDescuento tipo;
    private BigDecimal valor;
    private LocalDateTime fechaExpiracion;
    private Integer limiteUsos;
    private int usosActuales;
    private boolean activo;
    private LocalDateTime creadoEn;
}
