package com.ecommerce.modulos.resenas.infrastructure;

import com.ecommerce.modulos.resenas.domain.*;
import com.ecommerce.modulos.compartido.domain.ExcepcionEntidadNoEncontrada;
import com.ecommerce.modulos.compartido.domain.Calificacion;
import com.ecommerce.modulos.compartido.infrastructure.ContextoInquilino;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/productos/{idProducto}")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Producto resenas and ratings")
public class ControladorResena {

    private final RepositorioResena repositorioResena;

    @GetMapping("/resenas")
    @Operation(summary = "List producto resenas")
    public ResponseEntity<?> listReviews(
            @PathVariable UUID idProducto,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(repositorioResena.findAllByIdProductoAndActivoTrue(idProducto, PageRequest.of(page, size)));
    }

    @PostMapping("/resenas")
    @Operation(summary = "Create a resenas")
    public ResponseEntity<Resena> createReview(
            @PathVariable UUID idProducto, 
            @RequestBody CreateReviewRequest request,
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.ecommerce.modulos.identidad.application.DetallesUsuarioPersonalizado userDetails) {
        Resena resenas = new Resena();
        resenas.setIdTienda(ContextoInquilino.getIdTienda());
        resenas.setIdProducto(idProducto);
        if (userDetails != null) {
            resenas.setIdCliente(userDetails.getUserId());
        }
        resenas.setIdOrden(request.getIdOrden());
        resenas.setCalificacion(Calificacion.of(request.getCalificacion()));
        resenas.setTitulo(request.getTitulo());
        resenas.setComentario(request.getComentario());
        resenas = repositorioResena.save(resenas);
        return ResponseEntity.status(HttpStatus.CREATED).body(resenas);
    }

    @lombok.Data
    static class CreateReviewRequest {
        private UUID idOrden;
        private int calificacion;
        private String titulo;
        private String comentario;
    }
}
