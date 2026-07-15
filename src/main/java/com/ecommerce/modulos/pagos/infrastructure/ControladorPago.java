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

@RestController
@RequestMapping("/api/v1/pagos")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Pago processing")
public class ControladorPago {

    private final RepositorioPago repositorioPago;

    @PostMapping("/checkout/{idOrden}")
    @Operation(summary = "Create a checkout session for an ordenes")
    public ResponseEntity<Map<String, Object>> checkout(@PathVariable UUID idOrden) {
        Pago pagos = new Pago();
        pagos.setIdTienda(ContextoInquilino.getIdTienda());
        pagos.setIdOrden(idOrden);
        pagos.setAmount(new BigDecimal("0"));
        pagos.setMoneda("USD");
        pagos.setMetodoPago("STRIPE");
        pagos.setEstado(EstadoPago.PENDING);
        pagos.setIdExterno("simulated-checkout-" + UUID.randomUUID());
        repositorioPago.save(pagos);

        return ResponseEntity.ok(Map.of(
                "paymentId", pagos.getId(),
                "checkoutUrl", "https://checkout.stripe.com/pay/simulated"
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get pagos details")
    public ResponseEntity<Pago> getPayment(@PathVariable UUID id) {
        Pago pagos = repositorioPago.findById(id)
                .orElseThrow(() -> new ExcepcionEntidadNoEncontrada("Pago", id));
        return ResponseEntity.ok(pagos);
    }
}
