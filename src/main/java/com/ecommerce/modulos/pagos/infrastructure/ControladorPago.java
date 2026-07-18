package com.ecommerce.modulos.pagos.infrastructure;

import com.ecommerce.modulos.pagos.domain.*;
import com.ecommerce.modulos.compartido.domain.ExcepcionEntidadNoEncontrada;
import com.ecommerce.modulos.compartido.infrastructure.ContextoInquilino;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import com.ecommerce.modulos.pagos.application.CasoUsoProcesarPago;
import com.ecommerce.modulos.identidad.application.DetallesUsuarioPersonalizado;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/v1/pagos")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Pago processing")
public class ControladorPago {

    private final RepositorioPago repositorioPago;
    private final CasoUsoProcesarPago casoUsoProcesarPago;

    @PostMapping("/checkout/{idOrden}")
    @Operation(summary = "Create a checkout session for an ordenes")
    public ResponseEntity<Map<String, Object>> checkout(
            @PathVariable UUID idOrden,
            @AuthenticationPrincipal DetallesUsuarioPersonalizado userDetails) {
        
        if (userDetails == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
        }

        Map<String, Object> respuesta = casoUsoProcesarPago.ejecutar(idOrden, userDetails.getUserId());
        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get pagos details")
    public ResponseEntity<Pago> getPayment(@PathVariable UUID id) {
        Pago pagos = repositorioPago.findById(id)
                .orElseThrow(() -> new ExcepcionEntidadNoEncontrada("Pago", id));
        return ResponseEntity.ok(pagos);
    }
}
