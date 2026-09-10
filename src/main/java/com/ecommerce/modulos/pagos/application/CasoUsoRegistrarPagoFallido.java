package com.ecommerce.modulos.pagos.application;

import com.ecommerce.modulos.compartido.domain.ExcepcionEntidadNoEncontrada;
import com.ecommerce.modulos.ordenes.application.ServicioEstadoOrden;
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
 * <p>Marca el {@link Pago} como {@code FAILED} y, en la misma transacción, pide a
 * {@link ServicioEstadoOrden} que cancele la orden si todavía no se pagó — esa cancelación
 * emite {@code EventoInventarioLiberado}, que catálogo consume para reponer el stock reservado.
 * Antes el webhook solo marcaba el pago y dejaba la orden colgada en {@code PENDING} con el
 * inventario reservado para siempre.
 *
 * <p>Idempotente: si el pago ya está {@code FAILED} no hace nada; si ya está {@code COMPLETED}
 * se ignora el evento (llegó fuera de orden) y se deja rastro en el log.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CasoUsoRegistrarPagoFallido {

    private final RepositorioPago repositorioPago;
    private final ServicioEstadoOrden servicioEstadoOrden;

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

        servicioEstadoOrden.cancelarPorFalloDePago(pago.getIdOrden(), motivo);
    }
}
