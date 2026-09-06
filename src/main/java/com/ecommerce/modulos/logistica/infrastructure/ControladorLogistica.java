package com.ecommerce.modulos.logistica.infrastructure;

import com.ecommerce.modulos.compartido.infrastructure.ContextoInquilino;
import com.ecommerce.modulos.logistica.application.CasoUsoLogistica;
import com.ecommerce.modulos.logistica.domain.Envio;
import com.ecommerce.modulos.logistica.domain.EstadoEnvio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/logistica")
@RequiredArgsConstructor
@Tag(name = "Logística y Envíos", description = "Gestión de despachos y rastreo de paquetes")
public class ControladorLogistica {

    private final CasoUsoLogistica casoUsoLogistica;

    @GetMapping("/rastreo/{numeroGuia}")
    @Operation(summary = "Rastrear el estado de un envío usando el número de guía")
    public ResponseEntity<Envio> rastrearEnvio(@PathVariable String numeroGuia) {
        Envio envio = casoUsoLogistica.rastrearEnvio(ContextoInquilino.getIdTienda(), numeroGuia);
        return ResponseEntity.ok(envio);
    }

    @PostMapping("/admin/rastreo/{numeroGuia}/estado")
    @Operation(summary = "Actualizar estado de envío (Uso de proveedores/Admin)")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<Envio> actualizarEstado(
            @PathVariable String numeroGuia,
            @RequestParam EstadoEnvio estado,
            @RequestParam String ubicacion,
            @RequestParam String descripcion) {
        
        Envio envio = casoUsoLogistica.actualizarEstado(ContextoInquilino.getIdTiendaPropia(), numeroGuia, estado, ubicacion, descripcion);
        return ResponseEntity.ok(envio);
    }
}
