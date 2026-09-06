package com.ecommerce.modulos.ordenes.infrastructure;

import com.ecommerce.modulos.compartido.infrastructure.ContextoInquilino;
import com.ecommerce.modulos.compartido.infrastructure.RespuestaPaginada;
import com.ecommerce.modulos.ordenes.application.CasoUsoGestionarCupon;
import com.ecommerce.modulos.ordenes.application.dto.SolicitudCrearCupon;
import com.ecommerce.modulos.ordenes.domain.Cupon;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cupones")
@PreAuthorize("hasRole('SELLER')")
@RequiredArgsConstructor
@Tag(name = "Cupones", description = "Gestión de cupones y descuentos para Inquilinos")
public class ControladorCupon {

    private final CasoUsoGestionarCupon casoUsoGestionarCupon;

    @PostMapping
    @Operation(summary = "Crear un nuevo cupón de descuento")
    public ResponseEntity<Cupon> crearCupon(@Valid @RequestBody SolicitudCrearCupon request) {
        Cupon cupon = casoUsoGestionarCupon.crearCupon(ContextoInquilino.getIdTiendaPropia(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(cupon);
    }

    @GetMapping
    @Operation(summary = "Listar cupones de la tienda")
    public ResponseEntity<RespuestaPaginada<Cupon>> listarCupones(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(casoUsoGestionarCupon.listarCupones(
                ContextoInquilino.getIdTiendaPropia(),
                PageRequest.of(page, size)));
    }

    @PatchMapping("/{idCupon}/estado")
    @Operation(summary = "Activar o desactivar un cupón")
    public ResponseEntity<Void> alternarEstado(@PathVariable UUID idCupon) {
        casoUsoGestionarCupon.alternarEstadoCupon(ContextoInquilino.getIdTiendaPropia(), idCupon);
        return ResponseEntity.noContent().build();
    }
}
