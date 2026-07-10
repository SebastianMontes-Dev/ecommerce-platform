package com.ecommerce.modules.notification.application;

import com.ecommerce.modules.order.domain.Order;
import com.ecommerce.modules.order.domain.OrderRepository;
import com.ecommerce.modules.shared.domain.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationService {

    private final JavaMailSender mailSender;
    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public void sendOrderConfirmation(UUID orderId, UUID tenantId) {
        sendOrderEmail(orderId, "Pedido Confirmado",
                "Tu pedido %s ha sido confirmado y está siendo procesado.");
    }

    @Transactional(readOnly = true)
    public void sendPaymentReceived(UUID orderId, UUID tenantId) {
        sendOrderEmail(orderId, "Pago Recibido",
                "Hemos recibido el pago de tu pedido %s. ¡Gracias por tu compra!");
    }

    @Transactional(readOnly = true)
    public void sendOrderShipped(UUID orderId, UUID tenantId) {
        sendOrderEmail(orderId, "Pedido Enviado",
                "Tu pedido %s ha sido enviado. Pronto lo recibirás.");
    }

    @Transactional(readOnly = true)
    public void sendOrderDelivered(UUID orderId, UUID tenantId) {
        sendOrderEmail(orderId, "Pedido Entregado",
                "Tu pedido %s ha sido entregado. ¡Esperamos que disfrutes tu compra!");
    }

    @Transactional(readOnly = true)
    public void sendOrderCancelled(UUID orderId, UUID tenantId) {
        sendOrderEmail(orderId, "Pedido Cancelado",
                "Tu pedido %s ha sido cancelado. Si tienes preguntas, contáctanos.");
    }

    @Transactional(readOnly = true)
    public void sendOrderRefunded(UUID orderId, UUID tenantId) {
        sendOrderEmail(orderId, "Reembolso Procesado",
                "El reembolso de tu pedido %s ha sido procesado. El monto será devuelto en los próximos días.");
    }

    private void sendOrderEmail(UUID orderId, String subject, String bodyTemplate) {
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new EntityNotFoundException("Order", orderId));

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(order.getCustomerEmail());
            message.setSubject(subject + " - #" + order.getOrderNumber());
            message.setText(String.format(bodyTemplate, order.getOrderNumber()));
            message.setFrom("noreply@ecommerce.com");

            mailSender.send(message);
            log.info("Email sent to {} for order {}: {}", order.getCustomerEmail(), order.getOrderNumber(), subject);
        } catch (Exception e) {
            log.error("Failed to send email for order {}: {}", orderId, e.getMessage(), e);
        }
    }
}
