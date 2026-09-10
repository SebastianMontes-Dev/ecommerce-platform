package com.ecommerce.modulos.resenas.application.dto;

import com.ecommerce.modulos.resenas.domain.Resena;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record RespuestaResena(
        UUID id,
        UUID idProducto,
        BigDecimal calificacion,
        String titulo,
        String comentario,
        LocalDateTime creadoEn) {

    public static RespuestaResena de(Resena resena) {
        return new RespuestaResena(
                resena.getId(),
                resena.getIdProducto(),
                resena.getCalificacion() != null ? resena.getCalificacion().getValue() : null,
                resena.getTitulo(),
                resena.getComentario(),
                resena.getCreadoEn());
    }
}
