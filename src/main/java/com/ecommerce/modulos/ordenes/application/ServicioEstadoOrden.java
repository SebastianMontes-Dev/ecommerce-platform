package com.ecommerce.modulos.ordenes.application;

import com.ecommerce.modulos.compartido.domain.PublicadorEventoDominio;
import com.ecommerce.modulos.ordenes.domain.EstadoOrden;
import com.ecommerce.modulos.ordenes.domain.Orden;
import com.ecommerce.modulos.ordenes.domain.RepositorioOrden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * API de aplicación del módulo <b>ordenes</b> para que otros módulos (p. ej. <b>pagos</b>)
 * disparen transiciones del ciclo de vida de una orden <b>sin tocar su repositorio ni su
 * entidad</b>. Corre con propagación {@code REQUIRED}: cuando el webhook de pago lo invoca
 * dentro de su transacción, el cambio del pago y el de la orden commitean juntos.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ServicioEstadoOrden {

    private final RepositorioOrden repositorioOrden;
    private final PublicadorEventoDominio publicadorEventoDominio;

    /** Marca la orden como pagada. Idempotente: no-op si ya está {@code PAID}. */
    @Transactional
    public void marcarPagada(UUID idOrden) {
        Orden orden = repositorioOrden.findById(idOrden).orElse(null);
        if (orden == null) {
            log.warn("marcarPagada: orden {} no encontrada", idOrden);
            return;
        }
        if (orden.getEstado() == EstadoOrden.PAID) {
            return;
        }
        orden.markAsPaid();
        repositorioOrden.save(orden);
        publicarYLimpiar(orden);
        log.info("Orden {} marcada como PAID", idOrden);
    }

    /**
     * Cancela la orden porque su pago no prosperó (sesión expirada / rechazada). No-op si
     * la orden ya avanzó más allá de {@code PENDING}/{@code CONFIRMED}. La cancelación emite
     * {@code EventoInventarioLiberado}, que catálogo consume para reponer el stock reservado.
     */
    @Transactional
    public void cancelarPorFalloDePago(UUID idOrden, String motivo) {
        Orden orden = repositorioOrden.findById(idOrden).orElse(null);
        if (orden == null) {
            return;
        }
        if (orden.getEstado() != EstadoOrden.PENDING && orden.getEstado() != EstadoOrden.CONFIRMED) {
            log.warn("Orden {} en estado {}; no se cancela pese al pago fallido", idOrden, orden.getEstado());
            return;
        }
        orden.cancel(motivo);
        repositorioOrden.save(orden);
        publicarYLimpiar(orden);
        log.info("Orden {} cancelada por pago fallido; inventario en reposición", idOrden);
    }

    /**
     * Marca la orden como reembolsada. Idempotente: no-op si ya está {@code REFUNDED}. Emite
     * {@code EventoInventarioLiberado} (catálogo repone el stock).
     */
    @Transactional
    public void reembolsar(UUID idOrden, String motivo) {
        Orden orden = repositorioOrden.findById(idOrden)
                .orElseThrow(() -> new com.ecommerce.modulos.compartido.domain.ExcepcionEntidadNoEncontrada("Orden", idOrden));
        if (orden.getEstado() == EstadoOrden.REFUNDED) {
            return;
        }
        orden.refund(motivo);
        repositorioOrden.save(orden);
        publicarYLimpiar(orden);
        log.info("Orden {} reembolsada; inventario en reposición", idOrden);
    }

    private void publicarYLimpiar(Orden orden) {
        publicadorEventoDominio.publish(orden.getDomainEvents());
        orden.clearDomainEvents();
    }
}
