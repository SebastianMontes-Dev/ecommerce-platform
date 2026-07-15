package com.ecommerce.modulos.catalogo.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudCrearCategoria {

    @NotBlank
    private String nombre;

    @NotBlank
    private String enlaceCorto;

    private String descripcion;

    private String urlImagen;

    private java.util.UUID idPadre;
}
