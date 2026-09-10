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
 * Confirma un pago exitoso reportado por la pasarela (webhook de Stripe).
 *
 * <p>El cobro en Stripe ya ocurrió cuando este caso de uso corre; su responsabilidad es
 * reflejarlo <b>de forma atómica</b>: el {@link Pago} pasa a {@code COMPLETED} y la orden
 * a {@code PAID} en la misma transacción. La transición de la orden se delega en
 * {@link ServicioEstadoOrden} (API del módulo ordenes) — este módulo no toca el
 * repositorio ni la entidad {@code Orden}.
 *
 * <p>Idempotente: si el pago ya está {@code COMPLETED} (reintento de Stripe) no hace nada.
 * La preparación del envío y el registro de idempotencia del evento se hacen <i>fuera</i>
 * de esta transacción.
 *
 * <p>Requiere que el llamador ya haya fijado {@code ContextoInquilino}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CasoUsoConfirmarPago {

    private final RepositorioPago repositorioPago;
    private final ServicioEstadoOrden servicioEstadoOrden;

    /**
     * @param idPago            id interno del pago a confirmar
     * @param idExternoPasarela id definitivo de la pasarela (PaymentIntent); puede ser {@code null}
     * @return el id de la orden pagada, para preparar el envío fuera de la transacción
     */
    @Transactional
    public UUID confirmarPagoExitoso(UUID idPago, String idExternoPasarela) {
        Pago pago = repositorioPago.findById(idPago)
                .orElseThrow(() -> new ExcepcionEntidadNoEncontrada("Pago", idPago));

        if (pago.getEstado() == EstadoPago.COMPLETED) {
            log.info("Pago {} ya estaba COMPLETED; webhook duplicado, no se re-procesa", idPago);
            return pago.getIdOrden();
        }

        if (idExternoPasarela != null) {
            pago.setIdExterno(idExternoPasarela);
        }
        pago.complete();
        repositorioPago.save(pago);

        servicioEstadoOrden.marcarPagada(pago.getIdOrden());

        log.info("Pago {} confirmado; orden {} marcada como pagada", idPago, pago.getIdOrden());
        return pago.getIdOrden();
    }
}
