package com.ecommerce.modulos.logistica.infrastructure;

import com.ecommerce.modulos.compartido.infrastructure.ContextoInquilino;
import com.ecommerce.modulos.logistica.application.CasoUsoLogistica;
import com.ecommerce.modulos.logistica.application.dto.RespuestaEnvio;
import com.ecommerce.modulos.logistica.domain.EstadoEnvio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/logistica")
@RequiredArgsConstructor
@Tag(name = "Logística y Envíos", description = "Gestión de despachos y rastreo de paquetes")
public class ControladorLogistica {

    private final CasoUsoLogistica casoUsoLogistica;

    @GetMapping("/rastreo/{numeroGuia}")
    @Operation(summary = "Rastrear el estado de un envío usando el número de guía")
    public ResponseEntity<RespuestaEnvio> rastrearEnvio(@PathVariable String numeroGuia) {
        return ResponseEntity.ok(casoUsoLogistica.rastrearEnvio(ContextoInquilino.getIdTienda(), numeroGuia));
    }

    @PostMapping("/admin/rastreo/{numeroGuia}/estado")
    @Operation(summary = "Actualizar estado de envío (Uso de proveedores/Admin)")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<RespuestaEnvio> actualizarEstado(
            @PathVariable String numeroGuia,
            @RequestParam EstadoEnvio estado,
            @RequestParam String ubicacion,
            @RequestParam String descripcion) {
        return ResponseEntity.ok(casoUsoLogistica.actualizarEstado(
                ContextoInquilino.getIdTiendaPropia(), numeroGuia, estado, ubicacion, descripcion));
    }
}
