package com.ecommerce.modulos.pagos.application;

import com.ecommerce.modulos.compartido.domain.ExcepcionEntidadNoEncontrada;
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
 * Confirma un pago exitoso reportado por la pasarela (webhook de Stripe).
 *
 * <p>El cobro en Stripe ya ocurrió cuando este caso de uso corre; su única
 * responsabilidad es reflejarlo en nuestra base de datos <b>de forma atómica</b>:
 * el {@link Pago} pasa a {@code COMPLETED} y la {@link Orden} a {@code PAID} en la
 * misma transacción. Antes esto vivía suelto en el controlador del webhook con
 * varios {@code save()} sin transacción, de modo que un fallo intermedio dejaba el
 * pago cobrado y la orden sin actualizar.
 *
 * <p>Es idempotente: si el pago ya está {@code COMPLETED} (reintento de Stripe) no
 * hace nada. La preparación del envío y el registro de idempotencia del evento se
 * hacen <i>fuera</i> de esta transacción — un fallo de logística no debe revertir
 * un cobro real ni provocar reintentos infinitos del webhook.
 *
 * <p>Requiere que el llamador ya haya fijado {@code ContextoInquilino} (el webhook
 * resuelve la tienda con {@code findByIdExternoSinFiltro} antes de invocar).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CasoUsoConfirmarPago {

    private final RepositorioPago repositorioPago;
    private final RepositorioOrden repositorioOrden;

    /**
     * @param idPago          id interno del pago a confirmar
     * @param idExternoPasarela id definitivo de la pasarela (PaymentIntent de Stripe);
     *                          puede ser {@code null} si la pasarela no lo provee
     * @return el id de la orden pagada, para que el llamador prepare el envío fuera de la transacción
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

        Orden orden = repositorioOrden.findById(pago.getIdOrden())
                .orElseThrow(() -> new ExcepcionEntidadNoEncontrada("Orden", pago.getIdOrden()));

        if (orden.getEstado() != EstadoOrden.PAID) {
            orden.markAsPaid();
            repositorioOrden.save(orden);
        }

        log.info("Pago {} confirmado y orden {} marcada como PAID", idPago, orden.getId());
        return orden.getId();
    }
}
