package com.ecommerce.modulos.inquilino.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RespuestaInquilino {

    private UUID id;
    private String nombre;
    private String enlaceCorto;
    private String descripcion;
    private String urlLogo;
    private String urlBanner;
    private String estado;
    private UUID idPropietario;
    private LocalDateTime creadoEn;
}
