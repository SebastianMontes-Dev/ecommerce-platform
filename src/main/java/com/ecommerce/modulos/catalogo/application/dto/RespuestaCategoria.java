package com.ecommerce.modulos.catalogo.application.dto;

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
public class RespuestaCategoria {

    private UUID id;
    private String nombre;
    private String enlaceCorto;
    private String descripcion;
    private String urlImagen;
    private UUID idPadre;
    private List<RespuestaCategoria> children;
}
