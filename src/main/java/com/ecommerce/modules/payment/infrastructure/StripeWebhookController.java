package com.ecommerce.modules.payment.infrastructure;

import com.ecommerce.modules.payment.domain.Payment;
import com.ecommerce.modules.payment.domain.PaymentRepository;
import com.ecommerce.modules.payment.domain.PaymentStatus;
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
            log.error("Invalid Stripe webhook signature", e);
            return ResponseEntity.badRequest().body("Invalid signature");
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
                log.debug("Unhandled event type: {}", event.getType());
        }

        return ResponseEntity.ok("OK");
    }

    private void handleCheckoutCompleted(Event event) {
        Session session = (Session) event.getDataObjectDeserializer()
                .getObject()
                .orElse(null);

        if (session == null) return;

        String clientReferenceId = session.getClientReferenceId();
        if (clientReferenceId == null) return;

        paymentRepository.findByExternalId(clientReferenceId).ifPresent(payment -> {
            payment.setStatus(PaymentStatus.COMPLETED);
            paymentRepository.save(payment);
            log.info("Payment completed: {}", payment.getId());
        });
    }

    private void handleCheckoutExpired(Event event) {
        Session session = (Session) event.getDataObjectDeserializer()
                .getObject()
                .orElse(null);

        if (session == null) return;

        String clientReferenceId = session.getClientReferenceId();
        if (clientReferenceId == null) return;

        paymentRepository.findByExternalId(clientReferenceId).ifPresent(payment -> {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            log.info("Payment expired: {}", payment.getId());
        });
    }

    private void handlePaymentFailed(Event event) {
        com.stripe.model.PaymentIntent intent = (com.stripe.model.PaymentIntent) event.getDataObjectDeserializer()
                .getObject()
                .orElse(null);

        if (intent == null) return;

        paymentRepository.findByExternalId(intent.getId()).ifPresent(payment -> {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            log.info("Payment failed: {}", payment.getId());
        });
    }
}
