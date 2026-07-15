package com.ecommerce.modules.catalog.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {

    private UUID id;
    private String nombre;
    private String slug;
    private String descripcion;
    private String urlImagen;
    private UUID idPadre;
    private List<CategoryResponse> children;
}
