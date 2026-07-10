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
        log.info("Order {} status changed: {} -> {}", event.getOrderId(), event.getOldStatus(), event.getNewStatus());

        switch (event.getNewStatus()) {
            case CONFIRMED -> emailNotificationService.sendOrderConfirmation(event.getOrderId(), event.getTenantId());
            case PAID -> emailNotificationService.sendPaymentReceived(event.getOrderId(), event.getTenantId());
            case SHIPPED -> emailNotificationService.sendOrderShipped(event.getOrderId(), event.getTenantId());
            case DELIVERED -> emailNotificationService.sendOrderDelivered(event.getOrderId(), event.getTenantId());
            case CANCELLED -> emailNotificationService.sendOrderCancelled(event.getOrderId(), event.getTenantId());
            case REFUNDED -> emailNotificationService.sendOrderRefunded(event.getOrderId(), event.getTenantId());
            default -> log.debug("No notification configured for status: {}", event.getNewStatus());
        }
    }
}
