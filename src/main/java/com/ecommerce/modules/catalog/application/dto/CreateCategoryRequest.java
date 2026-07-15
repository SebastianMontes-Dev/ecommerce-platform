package com.ecommerce.modules.catalog.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCategoryRequest {

    @NotBlank
    private String nombre;

    @NotBlank
    private String slug;

    private String descripcion;

    private String urlImagen;

    private java.util.UUID idPadre;
}
