package com.ecommerce.modulos.notificacion.application;

import com.ecommerce.modulos.ordenes.domain.Orden;
import com.ecommerce.modulos.ordenes.domain.RepositorioOrden;
import com.ecommerce.modulos.compartido.domain.ExcepcionEntidadNoEncontrada;
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
public class ServicioNotificacionCorreo {

    private final JavaMailSender mailSender;
    private final RepositorioOrden repositorioOrden;

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
            Orden ordenes = repositorioOrden.findById(idOrden)
                    .orElseThrow(() -> new ExcepcionEntidadNoEncontrada("Orden", idOrden));

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(ordenes.getCorreoCliente());
            message.setSubject(subject + " - #" + ordenes.getNumeroOrden());
            message.setText(String.format(bodyTemplate, ordenes.getNumeroOrden()));
            message.setFrom("noreply@ecommerce.com");

            mailSender.send(message);
            log.info("Correo sent to {} for ordenes {}: {}", ordenes.getCorreoCliente(), ordenes.getNumeroOrden(), subject);
        } catch (Exception e) {
            log.error("Failed to send correo for ordenes {}: {}", idOrden, e.getMessage(), e);
        }
    }
}
