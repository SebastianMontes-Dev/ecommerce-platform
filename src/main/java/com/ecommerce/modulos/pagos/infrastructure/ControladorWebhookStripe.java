package com.ecommerce.modulos.pagos.infrastructure;

import com.ecommerce.modulos.pagos.domain.Pago;
import com.ecommerce.modulos.pagos.domain.RepositorioPago;
import com.ecommerce.modulos.pagos.domain.EstadoPago;
import com.ecommerce.modulos.pagos.domain.EventoProcesado;
import com.ecommerce.modulos.pagos.domain.RepositorioEventoProcesado;
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
@RequestMapping("/api/v1/pagos/webhook")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payments", description = "Pago webhook handlers")
public class ControladorWebhookStripe {

    private final RepositorioPago repositorioPago;
    private final RepositorioEventoProcesado repositorioEventoProcesado;

    @Value("${app.stripe.webhook-secret}")
    private String webhookSecret;

    @Value("${app.stripe.api-key}")
    private String stripeApiKey;

    @PostMapping("/stripe")
    @Operation(summary = "Handle Stripe webhook eventos")
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
        Event evento;

        try {
            evento = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.error("Firma de webhook inválida", e);
            return ResponseEntity.badRequest().body("Firma inválida");
        }

        // Idempotency check
        if (repositorioEventoProcesado.existsById(evento.getId())) {
            log.info("Evento webhook duplicado ignorado: {}", evento.getId());
            return ResponseEntity.ok("Ya procesado");
        }

        switch (evento.getType()) {
            case "checkout.session.completed":
                handleCheckoutCompleted(evento);
                break;
            case "checkout.session.expired":
                handleCheckoutExpired(evento);
                break;
            case "payment_intent.payment_failed":
                handlePaymentFailed(evento);
                break;
            default:
                log.debug("Tipo de evento no manejado: {}", evento.getType());
        }

        // Mark evento as processed
        repositorioEventoProcesado.save(new EventoProcesado(evento.getId(), evento.getType()));

        return ResponseEntity.ok("OK");
    }

    private void handleCheckoutCompleted(Event evento) {
        Session session = (Session) evento.getDataObjectDeserializer()
                .getObject()
                .orElse(null);

        if (session == null) return;

        String idReferenciaCliente = session.getClientReferenceId();
        if (idReferenciaCliente == null) return;

        Pago pagos = repositorioPago.findByIdExterno(idReferenciaCliente)
                .orElseThrow(() -> new IllegalStateException("Pago no encontrado para la sesión de Stripe con idReferenciaCliente: " + idReferenciaCliente));

        pagos.setIdExterno(session.getPaymentIntent());
        pagos.complete();
        repositorioPago.save(pagos);
        log.info("Pago completado exitosamente para la orden: {}", pagos.getIdOrden());
    }

    private void handleCheckoutExpired(Event evento) {
        Session session = (Session) evento.getDataObjectDeserializer()
                .getObject()
                .orElse(null);

        if (session == null) return;

        String idReferenciaCliente = session.getClientReferenceId();
        if (idReferenciaCliente == null) return;

        repositorioPago.findByIdExterno(idReferenciaCliente).ifPresent(pagos -> {
            pagos.setEstado(EstadoPago.FAILED);
            repositorioPago.save(pagos);
            log.info("Pago expirado: {}", pagos.getId());
        });
    }

    private void handlePaymentFailed(Event evento) {
        com.stripe.model.PaymentIntent intent = (com.stripe.model.PaymentIntent) evento.getDataObjectDeserializer()
                .getObject()
                .orElse(null);

        if (intent == null) return;

        repositorioPago.findByIdExterno(intent.getId()).ifPresent(pagos -> {
            pagos.setEstado(EstadoPago.FAILED);
            repositorioPago.save(pagos);
            log.info("Pago fallido: {}", pagos.getId());
        });
    }
}
