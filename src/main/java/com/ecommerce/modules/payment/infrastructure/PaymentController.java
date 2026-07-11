package com.ecommerce.modules.payment.infrastructure;

import com.ecommerce.modules.payment.domain.*;
import com.ecommerce.modules.shared.domain.EntityNotFoundException;
import com.ecommerce.modules.shared.infrastructure.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment processing")
public class PaymentController {

    private final PaymentRepository paymentRepository;

    @PostMapping("/checkout/{idOrden}")
    @Operation(summary = "Create a checkout session for an order")
    public ResponseEntity<Map<String, Object>> checkout(@PathVariable UUID idOrden) {
        Payment payment = new Payment();
        payment.setIdTienda(TenantContext.getIdTienda());
        payment.setIdOrden(idOrden);
        payment.setAmount(new BigDecimal("0"));
        payment.setCurrency("USD");
        payment.setPaymentMethod("STRIPE");
        payment.setEstado(PaymentStatus.PENDING);
        payment.setExternalId("simulated-checkout-" + UUID.randomUUID());
        paymentRepository.save(payment);

        return ResponseEntity.ok(Map.of(
                "paymentId", payment.getId(),
                "checkoutUrl", "https://checkout.stripe.com/pay/simulated"
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get payment details")
    public ResponseEntity<Payment> getPayment(@PathVariable UUID id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Payment", id));
        return ResponseEntity.ok(payment);
    }
}
