package com.ecommerce.modules.payment.infrastructure;

import com.ecommerce.modules.payment.domain.Payment;
import com.ecommerce.modules.payment.domain.PaymentRepository;
import com.ecommerce.modules.payment.domain.PaymentStatus;
import com.ecommerce.modules.payment.domain.ProcessedEvent;
import com.ecommerce.modules.payment.domain.ProcessedEventRepository;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments/webhook")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payments", description = "Payment webhook handlers")
public class StripeWebhookController {

    private final PaymentRepository paymentRepository;
    private final ProcessedEventRepository processedEventRepository;

    @Value("${app.stripe.webhook-secret}")
    private String webhookSecret;

    @Value("${app.stripe.api-key}")
    private String stripeApiKey;

    @PostMapping("/stripe")
    @Operation(summary = "Handle Stripe webhook events")
    public ResponseEntity<String> handleStripeWebhook(HttpServletRequest request) {
        String payload;
        try {
            BufferedReader reader = request.getReader();
            StringBuilder sb = new StringBuilder();
            reader.lines().forEach(sb::append);
            payload = sb.toString();
        } catch (Exception e) {
            log.error("Failed to read webhook payload", e);
            return ResponseEntity.badRequest().body("Invalid payload");
        }

        String sigHeader = request.getHeader("Stripe-Signature");
        Event event;

        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.error("Firma de webhook inválida", e);
            return ResponseEntity.badRequest().body("Firma inválida");
        }

        // Idempotency check
        if (processedEventRepository.existsById(event.getId())) {
            log.info("Evento webhook duplicado ignorado: {}", event.getId());
            return ResponseEntity.ok("Ya procesado");
        }

        switch (event.getType()) {
            case "checkout.session.completed":
                handleCheckoutCompleted(event);
                break;
            case "checkout.session.expired":
                handleCheckoutExpired(event);
                break;
            case "payment_intent.payment_failed":
                handlePaymentFailed(event);
                break;
            default:
                log.debug("Tipo de evento no manejado: {}", event.getType());
        }

        // Mark event as processed
        processedEventRepository.save(new ProcessedEvent(event.getId(), event.getType()));

        return ResponseEntity.ok("OK");
    }

    private void handleCheckoutCompleted(Event event) {
        Session session = (Session) event.getDataObjectDeserializer()
                .getObject()
                .orElse(null);

        if (session == null) return;

        String idReferenciaCliente = session.getClientReferenceId();
        if (idReferenciaCliente == null) return;

        Payment payment = paymentRepository.findByExternalId(idReferenciaCliente)
                .orElseThrow(() -> new IllegalStateException("Pago no encontrado para la sesión de Stripe con idReferenciaCliente: " + idReferenciaCliente));

        payment.setExternalId(session.getPaymentIntent());
        payment.complete();
        paymentRepository.save(payment);
        log.info("Pago completado exitosamente para la orden: {}", payment.getIdOrden());
    }

    private void handleCheckoutExpired(Event event) {
        Session session = (Session) event.getDataObjectDeserializer()
                .getObject()
                .orElse(null);

        if (session == null) return;

        String idReferenciaCliente = session.getClientReferenceId();
        if (idReferenciaCliente == null) return;

        paymentRepository.findByExternalId(idReferenciaCliente).ifPresent(payment -> {
            payment.setEstado(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            log.info("Pago expirado: {}", payment.getId());
        });
    }

    private void handlePaymentFailed(Event event) {
        com.stripe.model.PaymentIntent intent = (com.stripe.model.PaymentIntent) event.getDataObjectDeserializer()
                .getObject()
                .orElse(null);

        if (intent == null) return;

        paymentRepository.findByExternalId(intent.getId()).ifPresent(payment -> {
            payment.setEstado(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            log.info("Pago fallido: {}", payment.getId());
        });
    }
}
