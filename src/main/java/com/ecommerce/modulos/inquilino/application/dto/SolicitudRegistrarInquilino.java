package com.ecommerce.modulos.inquilino.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudRegistrarInquilino {

    @NotBlank(message = "Store nombre is required")
    @Size(min = 2, max = 100)
    private String nombre;

    @NotBlank(message = "Store enlaceCorto is required")
    @Size(min = 2, max = 50)
    private String enlaceCorto;

    private String descripcion;
}
