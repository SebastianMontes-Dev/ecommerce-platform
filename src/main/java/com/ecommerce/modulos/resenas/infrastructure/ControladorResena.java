package com.ecommerce.modulos.resenas.infrastructure;

import com.ecommerce.modulos.compartido.infrastructure.RespuestaPaginada;
import com.ecommerce.modulos.identidad.application.DetallesUsuarioPersonalizado;
import com.ecommerce.modulos.resenas.application.CasoUsoConsultarResenas;
import com.ecommerce.modulos.resenas.application.CasoUsoCrearResena;
import com.ecommerce.modulos.resenas.application.dto.RespuestaResena;
import com.ecommerce.modulos.resenas.application.dto.SolicitudCrearResena;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/productos/{idProducto}")
@RequiredArgsConstructor
@Tag(name = "Reseñas", description = "Reseñas y calificaciones de productos")
public class ControladorResena {

    private final CasoUsoConsultarResenas casoUsoConsultarResenas;
    private final CasoUsoCrearResena casoUsoCrearResena;

    @GetMapping("/resenas")
    @Operation(summary = "Listar reseñas de un producto")
    public ResponseEntity<RespuestaPaginada<RespuestaResena>> listarResenas(
            @PathVariable UUID idProducto,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(
                casoUsoConsultarResenas.listarActivasDeProducto(idProducto, PageRequest.of(page, size)));
    }

    @PostMapping("/resenas")
    @Operation(summary = "Crear una nueva reseña")
    public ResponseEntity<RespuestaResena> crearResena(
            @PathVariable UUID idProducto,
            @Valid @RequestBody SolicitudCrearResena solicitud,
            @AuthenticationPrincipal DetallesUsuarioPersonalizado userDetails) {

        UUID idCliente = (userDetails != null) ? userDetails.getUserId() : null;
        RespuestaResena resena = casoUsoCrearResena.ejecutar(idProducto, idCliente, solicitud);
        return ResponseEntity.status(HttpStatus.CREATED).body(resena);
    }
}
