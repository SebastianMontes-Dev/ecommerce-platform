package com.ecommerce.modulos.catalogo.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RespuestaImagenProducto {

    private UUID id;
    private String url;
    private String altText;
    private Integer width;
    private Integer height;
    private int sortOrder;
}
