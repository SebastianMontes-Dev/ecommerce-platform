package com.ecommerce.modulos.ordenes.application.dto;

import com.ecommerce.modulos.ordenes.domain.TipoDescuento;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudCrearCupon {

    @NotBlank(message = "El código del cupón es obligatorio")
    private String codigo;

    @NotNull(message = "El tipo de descuento es obligatorio")
    private TipoDescuento tipo;

    @NotNull(message = "El valor del descuento es obligatorio")
    @Positive(message = "El valor debe ser positivo")
    private BigDecimal valor;

    @Future(message = "La fecha de expiración debe estar en el futuro")
    private LocalDateTime fechaExpiracion;

    @Positive(message = "El límite de usos debe ser mayor a cero")
    private Integer limiteUsos;
}
