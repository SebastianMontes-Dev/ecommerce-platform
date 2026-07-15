package com.ecommerce.modulos.ordenes.infrastructure;

import com.ecommerce.modulos.ordenes.application.*;
import com.ecommerce.modulos.compartido.infrastructure.ContextoInquilino;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ordenes")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Orden management")
public class ControladorOrden {

    private final CasoUsoOrden casoUsoOrden;

    @GetMapping
    @Operation(summary = "List ordenes for current inquilino")
    public ResponseEntity<?> listOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(casoUsoOrden.listOrdersByTenant(
                ContextoInquilino.getIdTienda(),
                org.springframework.data.domain.PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get ordenes by ID")
    public ResponseEntity<RespuestaOrden> getOrder(@PathVariable UUID id) {
        return ResponseEntity.ok(casoUsoOrden.getOrder(id, ContextoInquilino.getIdTienda()));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel an ordenes")
    public ResponseEntity<RespuestaOrden> cancelOrder(@PathVariable UUID id, @RequestParam(defaultValue = "") String reason) {
        return ResponseEntity.ok(casoUsoOrden.cancelOrder(id, ContextoInquilino.getIdTienda(), reason));
    }
}
