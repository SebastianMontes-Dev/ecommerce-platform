package com.ecommerce.modulos.pagos.application;

import com.ecommerce.modulos.compartido.domain.ExcepcionEntidadNoEncontrada;
import com.ecommerce.modulos.compartido.domain.PublicadorEventoDominio;
import com.ecommerce.modulos.ordenes.domain.EstadoOrden;
import com.ecommerce.modulos.ordenes.domain.Orden;
import com.ecommerce.modulos.ordenes.domain.RepositorioOrden;
import com.ecommerce.modulos.pagos.domain.EstadoPago;
import com.ecommerce.modulos.pagos.domain.Pago;
import com.ecommerce.modulos.pagos.domain.RepositorioPago;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Registra que un pago no prosperó (sesión de Stripe expirada o PaymentIntent rechazado).
 *
 * <p>Antes el webhook solo marcaba el {@link Pago} como {@code FAILED} y dejaba la orden
 * colgada en {@code PENDING} <b>con el inventario reservado para siempre</b>. Aquí, en la
 * misma transacción, además se cancela la orden si todavía no se pagó; esa cancelación
 * emite {@code EventoOrdenCancelada}, que el módulo de catálogo consume para reponer el
 * stock (manejador simétrico al de la reserva).
 *
 * <p>Idempotente: si el pago ya está {@code FAILED} no hace nada; si ya está
 * {@code COMPLETED} se ignora el evento (llegó fuera de orden) y se deja rastro en el log.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CasoUsoRegistrarPagoFallido {

    private final RepositorioPago repositorioPago;
    private final RepositorioOrden repositorioOrden;
    private final PublicadorEventoDominio publicadorEventoDominio;

    @Transactional
    public void registrar(UUID idPago, String motivo) {
        Pago pago = repositorioPago.findById(idPago)
                .orElseThrow(() -> new ExcepcionEntidadNoEncontrada("Pago", idPago));

        if (pago.getEstado() == EstadoPago.FAILED) {
            log.info("Pago {} ya estaba FAILED; evento duplicado, no se re-procesa", idPago);
            return;
        }
        if (pago.getEstado() == EstadoPago.COMPLETED) {
            log.warn("Pago {} ya está COMPLETED; se ignora evento de fallo '{}' (llegó fuera de orden)", idPago, motivo);
            return;
        }

        pago.fail();
        repositorioPago.save(pago);

        repositorioOrden.findById(pago.getIdOrden()).ifPresent(orden -> cancelarSiProcede(orden, motivo));
    }

    private void cancelarSiProcede(Orden orden, String motivo) {
        if (orden.getEstado() != EstadoOrden.PENDING && orden.getEstado() != EstadoOrden.CONFIRMED) {
            log.warn("Orden {} en estado {}; no se cancela pese al pago fallido", orden.getId(), orden.getEstado());
            return;
        }
        orden.cancel(motivo);
        repositorioOrden.save(orden);
        publicadorEventoDominio.publish(orden.getDomainEvents());
        orden.clearDomainEvents();
        log.info("Orden {} cancelada por pago fallido; inventario en reposición", orden.getId());
    }
}
