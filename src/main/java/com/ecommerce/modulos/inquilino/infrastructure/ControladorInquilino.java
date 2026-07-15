package com.ecommerce.modulos.inquilino.infrastructure;

import com.ecommerce.modulos.inquilino.application.*;
import com.ecommerce.modulos.inquilino.application.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/inquilinos")
@RequiredArgsConstructor
@Tag(name = "Tenants", description = "Multi-inquilino store management")
public class ControladorInquilino {

    private final CasoUsoRegistrarInquilino casoUsoRegistrarInquilino;
    private final CasoUsoObtenerInquilino casoUsoObtenerInquilino;

    @PostMapping
    @Operation(summary = "Register a new store/inquilino")
    public ResponseEntity<RespuestaInquilino> register(@Valid @RequestBody SolicitudRegistrarInquilino request) {
        RespuestaInquilino response = casoUsoRegistrarInquilino.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{enlaceCorto}")
    @Operation(summary = "Get store/inquilino by enlaceCorto (public)")
    public ResponseEntity<RespuestaInquilino> getBySlug(@PathVariable String enlaceCorto) {
        RespuestaInquilino response = casoUsoObtenerInquilino.bySlug(enlaceCorto);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/yo")
    @Operation(summary = "Get my current store/inquilino")
    public ResponseEntity<RespuestaInquilino> getMyTenant() {
        RespuestaInquilino response = casoUsoObtenerInquilino.myTenant();
        return ResponseEntity.ok(response);
    }
}
