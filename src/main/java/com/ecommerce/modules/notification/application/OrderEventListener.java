package com.ecommerce.modules.notification.application;

import com.ecommerce.modules.order.domain.OrderStatus;
import com.ecommerce.modules.order.domain.events.OrderStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventListener {

    private final EmailNotificationService emailNotificationService;

    @Async
    @EventListener
    public void onOrderStatusChanged(OrderStatusChangedEvent event) {
        log.info("Order {} estado changed: {} -> {}", event.getIdOrden(), event.getEstadoAnterior(), event.getNuevoEstado());

        switch (event.getNuevoEstado()) {
            case CONFIRMED -> emailNotificationService.sendOrderConfirmation(event.getIdOrden(), event.getIdTienda());
            case PAID -> emailNotificationService.sendPaymentReceived(event.getIdOrden(), event.getIdTienda());
            case SHIPPED -> emailNotificationService.sendOrderShipped(event.getIdOrden(), event.getIdTienda());
            case DELIVERED -> emailNotificationService.sendOrderDelivered(event.getIdOrden(), event.getIdTienda());
            case CANCELLED -> emailNotificationService.sendOrderCancelled(event.getIdOrden(), event.getIdTienda());
            case REFUNDED -> emailNotificationService.sendOrderRefunded(event.getIdOrden(), event.getIdTienda());
            default -> log.debug("No notification configured for estado: {}", event.getNuevoEstado());
        }
    }
}
