package com.ecommerce.modulos.notificacion.application;

import com.ecommerce.modulos.ordenes.domain.Orden;
import com.ecommerce.modulos.ordenes.domain.RepositorioOrden;
import com.ecommerce.modulos.compartido.domain.ExcepcionEntidadNoEncontrada;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServicioNotificacionCorreo {

    private final JavaMailSender mailSender;
    private final RepositorioOrden repositorioOrden;
    private final TemplateEngine templateEngine;

    @Transactional(readOnly = true)
    public void sendOrderConfirmation(UUID idOrden, UUID idTienda) {
        sendOrderEmail(idOrden, "Pedido Confirmado",
                "Tu pedido ha sido confirmado y está siendo procesado.",
                "¡Gracias por tu compra!");
    }

    @Transactional(readOnly = true)
    public void sendPaymentReceived(UUID idOrden, UUID idTienda) {
        sendOrderEmail(idOrden, "Pago Recibido",
                "Hemos recibido el pago de tu pedido. Estamos preparando tu envío.",
                "¡Gracias por tu pago!");
    }

    @Transactional(readOnly = true)
    public void sendOrderShipped(UUID idOrden, UUID idTienda) {
        sendOrderEmail(idOrden, "Pedido Enviado",
                "Tu pedido ha sido enviado. Pronto lo recibirás en la dirección indicada.",
                "¡En camino!");
    }

    @Transactional(readOnly = true)
    public void sendOrderDelivered(UUID idOrden, UUID idTienda) {
        sendOrderEmail(idOrden, "Pedido Entregado",
                "Tu pedido ha sido entregado exitosamente.",
                "¡Esperamos que disfrutes tu compra!");
    }

    @Transactional(readOnly = true)
    public void sendOrderCancelled(UUID idOrden, UUID idTienda) {
        sendOrderEmail(idOrden, "Pedido Cancelado",
                "Tu pedido ha sido cancelado. Si tienes preguntas, por favor contáctanos.",
                "Orden Cancelada");
    }

    @Transactional(readOnly = true)
    public void sendOrderRefunded(UUID idOrden, UUID idTienda) {
        sendOrderEmail(idOrden, "Reembolso Procesado",
                "El reembolso de tu pedido ha sido procesado. El monto será devuelto a tu método de pago original en los próximos días.",
                "Reembolso Exitoso");
    }

    private void sendOrderEmail(UUID idOrden, String subject, String bodyMessage, String headline) {
        try {
            Orden orden = repositorioOrden.findById(idOrden)
                    .orElseThrow(() -> new ExcepcionEntidadNoEncontrada("Orden", idOrden));

            Context context = new Context();
            context.setVariable("headline", headline);
            context.setVariable("messageBody", bodyMessage);
            context.setVariable("orderNumber", orden.getNumeroOrden());
            context.setVariable("customerName", orden.getNombreCliente() != null ? orden.getNombreCliente() : "Cliente");
            context.setVariable("totalAmount", orden.getTotal().getAmount() + " " + orden.getTotal().getMoneda());

            String process = templateEngine.process("email/plantilla-orden", context);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            helper.setTo(orden.getCorreoCliente());
            helper.setSubject(subject + " - #" + orden.getNumeroOrden());
            helper.setText(process, true); // true indica que es HTML
            helper.setFrom("noreply@nexasaas.com");

            mailSender.send(mimeMessage);
            log.info("Correo HTML enviado a {} para la orden {}: {}", orden.getCorreoCliente(), orden.getNumeroOrden(), subject);
        } catch (Exception e) {
            log.error("Fallo al enviar correo HTML para la orden {}: {}", idOrden, e.getMessage(), e);
        }
    }
}
