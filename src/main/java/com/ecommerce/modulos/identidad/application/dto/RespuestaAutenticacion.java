package com.ecommerce.modulos.identidad.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RespuestaAutenticacion {

    private String accessToken;
    private String tokenActualizacion;
    private long expiresIn;
    private String tokenType;
}
