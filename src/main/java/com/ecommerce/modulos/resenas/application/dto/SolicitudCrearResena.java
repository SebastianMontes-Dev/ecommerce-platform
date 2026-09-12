package com.ecommerce.modulos.resenas.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class SolicitudCrearResena {
    
    @NotNull(message = "El ID de la orden es obligatorio")
    private UUID idOrden;
    
    @Min(value = 1, message = "La calificación mínima es 1")
    @Max(value = 5, message = "La calificación máxima es 5")
    private int calificacion;
    
    @NotBlank(message = "El título de la reseña no puede estar vacío")
    @Size(max = 200, message = "El título no puede superar los 200 caracteres")
    private String titulo;

    @NotBlank(message = "El comentario no puede estar vacío")
    @Size(max = 2000, message = "El comentario no puede superar los 2000 caracteres")
    private String comentario;
}
