package com.ecommerce.modulos.analiticas.infrastructure;

import com.ecommerce.modulos.analiticas.application.CasoUsoAnaliticas;
import com.ecommerce.modulos.analiticas.application.dto.ResumenDashboard;
import com.ecommerce.modulos.compartido.infrastructure.ContextoInquilino;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analiticas")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SELLER')")
@Tag(name = "Analíticas", description = "Dashboard de ventas y métricas para inquilinos")
public class ControladorAnaliticas {

    private final CasoUsoAnaliticas casoUsoAnaliticas;

    @GetMapping("/dashboard")
    @Operation(summary = "Obtener resumen de analíticas del Dashboard")
    public ResponseEntity<ResumenDashboard> obtenerDashboard() {
        ResumenDashboard resumen = casoUsoAnaliticas.obtenerResumen(ContextoInquilino.getIdTiendaPropia());
        return ResponseEntity.ok(resumen);
    }
}
