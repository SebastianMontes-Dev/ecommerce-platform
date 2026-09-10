package com.ecommerce.modulos.pagos.application;

import com.ecommerce.modulos.compartido.domain.ExcepcionEntidadNoEncontrada;
import com.ecommerce.modulos.compartido.infrastructure.ContextoInquilino;
import com.ecommerce.modulos.ordenes.domain.EstadoOrden;
import com.ecommerce.modulos.ordenes.domain.Orden;
import com.ecommerce.modulos.ordenes.domain.RepositorioOrden;
import com.ecommerce.modulos.pagos.domain.EstadoPago;
import com.ecommerce.modulos.pagos.domain.Pago;
import com.ecommerce.modulos.pagos.domain.RepositorioPago;
import com.ecommerce.modulos.catalogo.domain.RepositorioProducto;
import com.ecommerce.modulos.catalogo.domain.RepositorioVarianteProducto;
import com.stripe.exception.StripeException;
import com.stripe.model.Refund;
import com.stripe.param.RefundCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CasoUsoGestionarReembolso {

    private final RepositorioPago repositorioPago;
    private final RepositorioOrden repositorioOrden;
    private final RepositorioProducto repositorioProducto;
    private final RepositorioVarianteProducto repositorioVarianteProducto;

    @Transactional
    public void reembolsarOrden(UUID idOrden, String reason) {
        Orden orden = repositorioOrden.findById(idOrden)
                .orElseThrow(() -> new ExcepcionEntidadNoEncontrada("Orden", idOrden));

        if (orden.getEstado() == EstadoOrden.REFUNDED) {
            log.info("La orden {} ya está reembolsada.", idOrden);
            return;
        }
        
        if (orden.getEstado() == EstadoOrden.CANCELLED) {
             throw new IllegalStateException("No se puede reembolsar una orden cancelada sin pago.");
        }

        Pago pago = repositorioPago.findByIdOrden(idOrden)
                .orElseThrow(() -> new ExcepcionEntidadNoEncontrada("Pago", idOrden));

        if (pago.getEstado() != EstadoPago.COMPLETED) {
            throw new IllegalStateException("Solo se pueden reembolsar pagos completados.");
        }

        try {
            RefundCreateParams params = RefundCreateParams.builder()
                    .setPaymentIntent(pago.getIdExterno())
                    .setReason(RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER)
                    .build();

            Refund refund = Refund.create(params);
            log.info("Stripe Refund creado: {}", refund.getId());

            pago.setEstado(EstadoPago.REFUNDED);
            repositorioPago.save(pago);

            orden.refund(reason);
            repositorioOrden.save(orden);

            // Reponer inventario sobre filas bloqueadas (FOR UPDATE) para no pisar decrementos
            // concurrentes. Antes se hacía load-modify-save (racy) y solo para variantes: los
            // productos sin variante nunca recuperaban stock al reembolsar.
            orden.getArticulos().forEach(articulo -> {
                if (articulo.getVariantId() != null) {
                    repositorioVarianteProducto.findByIdForUpdate(articulo.getVariantId()).ifPresent(variante -> {
                        variante.increaseInventory(articulo.getCantidad());
                        repositorioVarianteProducto.save(variante);
                    });
                } else {
                    repositorioProducto.findByIdForUpdate(articulo.getIdProducto()).ifPresent(producto -> {
                        producto.increaseInventory(articulo.getCantidad());
                        repositorioProducto.save(producto);
                    });
                }
            });

        } catch (StripeException e) {
            log.error("Error al procesar reembolso en Stripe para el pago {}", pago.getId(), e);
            throw new RuntimeException("Error en Stripe al procesar el reembolso", e);
        }
    }
}
