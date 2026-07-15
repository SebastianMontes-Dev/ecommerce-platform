package com.ecommerce.modulos.identidad.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudActualizarToken {

    @NotBlank(message = "Refresh token is required")
    private String tokenActualizacion;
}
