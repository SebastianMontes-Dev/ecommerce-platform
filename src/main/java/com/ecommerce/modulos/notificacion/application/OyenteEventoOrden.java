package com.ecommerce.modulos.notificacion.application;

import com.ecommerce.modulos.ordenes.domain.EstadoOrden;
import com.ecommerce.modulos.ordenes.domain.eventos.EventoEstadoOrdenCambiado;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OyenteEventoOrden {

    private final ServicioNotificacionCorreo servicioNotificacionCorreo;

    @Async
    @EventListener
    public void onOrderStatusChanged(EventoEstadoOrdenCambiado evento) {
        log.info("Orden {} estado changed: {} -> {}", evento.getIdOrden(), evento.getEstadoAnterior(), evento.getNuevoEstado());

        switch (evento.getNuevoEstado()) {
            case CONFIRMED -> servicioNotificacionCorreo.sendOrderConfirmation(evento.getIdOrden(), evento.getIdTienda());
            case PAID -> servicioNotificacionCorreo.sendPaymentReceived(evento.getIdOrden(), evento.getIdTienda());
            case SHIPPED -> servicioNotificacionCorreo.sendOrderShipped(evento.getIdOrden(), evento.getIdTienda());
            case DELIVERED -> servicioNotificacionCorreo.sendOrderDelivered(evento.getIdOrden(), evento.getIdTienda());
            case CANCELLED -> servicioNotificacionCorreo.sendOrderCancelled(evento.getIdOrden(), evento.getIdTienda());
            case REFUNDED -> servicioNotificacionCorreo.sendOrderRefunded(evento.getIdOrden(), evento.getIdTienda());
            default -> log.debug("No notificacion configured for estado: {}", evento.getNuevoEstado());
        }
    }
}
