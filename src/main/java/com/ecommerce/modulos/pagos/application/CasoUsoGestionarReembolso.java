package com.ecommerce.modulos.pagos.application;

import com.ecommerce.modulos.compartido.domain.ExcepcionEntidadNoEncontrada;
import com.ecommerce.modulos.ordenes.application.ServicioEstadoOrden;
import com.ecommerce.modulos.pagos.domain.EstadoPago;
import com.ecommerce.modulos.pagos.domain.Pago;
import com.ecommerce.modulos.pagos.domain.RepositorioPago;
import com.stripe.exception.StripeException;
import com.stripe.model.Refund;
import com.stripe.param.RefundCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Reembolsa una orden pagada: ejecuta el refund en Stripe, marca el {@link Pago} como
 * {@code REFUNDED} y pide a {@link ServicioEstadoOrden} que pase la orden a {@code REFUNDED}
 * (que emite {@code EventoInventarioLiberado} → catálogo repone el stock, con el mismo
 * bloqueo pesimista que la cancelación). Este módulo ya no toca el repositorio de ordenes
 * ni el de catálogo.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CasoUsoGestionarReembolso {

    private final RepositorioPago repositorioPago;
    private final ServicioEstadoOrden servicioEstadoOrden;

    @Transactional
    public void reembolsarOrden(UUID idOrden, String motivo) {
        Pago pago = repositorioPago.findByIdOrden(idOrden)
                .orElseThrow(() -> new ExcepcionEntidadNoEncontrada("Pago", idOrden));

        if (pago.getEstado() == EstadoPago.REFUNDED) {
            log.info("El pago de la orden {} ya está reembolsado.", idOrden);
            return;
        }
        if (pago.getEstado() != EstadoPago.COMPLETED) {
            throw new IllegalStateException("Solo se pueden reembolsar pagos completados.");
        }

        try {
            Refund refund = Refund.create(RefundCreateParams.builder()
                    .setPaymentIntent(pago.getIdExterno())
                    .setReason(RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER)
                    .build());
            log.info("Stripe Refund creado: {}", refund.getId());
        } catch (StripeException e) {
            log.error("Error al procesar reembolso en Stripe para el pago {}", pago.getId(), e);
            throw new RuntimeException("Error en Stripe al procesar el reembolso", e);
        }

        pago.refund();
        repositorioPago.save(pago);

        // Lanza ExcepcionOperacionInvalida (409) si la orden no admite la transición a REFUNDED.
        servicioEstadoOrden.reembolsar(idOrden, motivo);
    }
}
