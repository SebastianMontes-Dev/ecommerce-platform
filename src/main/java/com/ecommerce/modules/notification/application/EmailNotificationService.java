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
    public void sendOrderConfirmation(UUID idOrden, UUID idTienda) {
        sendOrderEmail(idOrden, "Pedido Confirmado",
                "Tu pedido %s ha sido confirmado y está siendo procesado.");
    }

    @Transactional(readOnly = true)
    public void sendPaymentReceived(UUID idOrden, UUID idTienda) {
        sendOrderEmail(idOrden, "Pago Recibido",
                "Hemos recibido el pago de tu pedido %s. ¡Gracias por tu compra!");
    }

    @Transactional(readOnly = true)
    public void sendOrderShipped(UUID idOrden, UUID idTienda) {
        sendOrderEmail(idOrden, "Pedido Enviado",
                "Tu pedido %s ha sido enviado. Pronto lo recibirás.");
    }

    @Transactional(readOnly = true)
    public void sendOrderDelivered(UUID idOrden, UUID idTienda) {
        sendOrderEmail(idOrden, "Pedido Entregado",
                "Tu pedido %s ha sido entregado. ¡Esperamos que disfrutes tu compra!");
    }

    @Transactional(readOnly = true)
    public void sendOrderCancelled(UUID idOrden, UUID idTienda) {
        sendOrderEmail(idOrden, "Pedido Cancelado",
                "Tu pedido %s ha sido cancelado. Si tienes preguntas, contáctanos.");
    }

    @Transactional(readOnly = true)
    public void sendOrderRefunded(UUID idOrden, UUID idTienda) {
        sendOrderEmail(idOrden, "Reembolso Procesado",
                "El reembolso de tu pedido %s ha sido procesado. El monto será devuelto en los próximos días.");
    }

    private void sendOrderEmail(UUID idOrden, String subject, String bodyTemplate) {
        try {
            Order order = orderRepository.findById(idOrden)
                    .orElseThrow(() -> new EntityNotFoundException("Order", idOrden));

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(order.getCustomerEmail());
            message.setSubject(subject + " - #" + order.getOrderNumber());
            message.setText(String.format(bodyTemplate, order.getOrderNumber()));
            message.setFrom("noreply@ecommerce.com");

            mailSender.send(message);
            log.info("Email sent to {} for order {}: {}", order.getCustomerEmail(), order.getOrderNumber(), subject);
        } catch (Exception e) {
            log.error("Failed to send correo for order {}: {}", idOrden, e.getMessage(), e);
        }
    }
}
