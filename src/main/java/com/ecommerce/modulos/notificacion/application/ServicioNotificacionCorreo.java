package com.ecommerce.modulos.notificacion.application;

import com.ecommerce.modulos.ordenes.application.ServicioConsultaOrden;
import com.ecommerce.modulos.ordenes.application.ServicioConsultaOrden.ResumenOrden;
import com.ecommerce.modulos.ordenes.application.dto.FacturaOrden;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

import java.util.UUID;

/**
 * Envía los correos transaccionales de una orden. Obtiene los datos que necesita del
 * módulo <b>ordenes</b> a través de su API de lectura ({@link ServicioConsultaOrden}),
 * no de su repositorio ni de su entidad.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ServicioNotificacionCorreo {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final ServicioConsultaOrden servicioConsultaOrden;
    private final ServicioFacturaPdf servicioFacturaPdf;

    @CircuitBreaker(name = "correos", fallbackMethod = "fallbackCorreo")
    public void sendOrderConfirmation(UUID idOrden, UUID idTienda) {
        sendOrderEmail(idOrden, "Pedido Confirmado",
                "Tu pedido ha sido confirmado y está siendo procesado.",
                "¡Gracias por tu compra!");
    }

    @CircuitBreaker(name = "correos", fallbackMethod = "fallbackCorreo")
    public void sendPaymentReceived(UUID idOrden, UUID idTienda) {
        FacturaOrden factura = servicioConsultaOrden.obtenerFactura(idOrden);
        try {
            byte[] pdfBytes = servicioFacturaPdf.generarPdf(factura);
            enviarConAdjunto(factura, "Pago Recibido",
                    "Hemos recibido el pago de tu pedido. Adjunto encontrarás la factura.",
                    "¡Gracias por tu pago!",
                    "factura_" + factura.getNumeroOrden() + ".pdf", pdfBytes, "application/pdf");
        } catch (Exception e) {
            log.error("Fallo al enviar correo con factura para la orden {}: {}", idOrden, e.getMessage(), e);
            throw new RuntimeException("Error en SMTP o Generacion PDF", e);
        }
    }

    @CircuitBreaker(name = "correos", fallbackMethod = "fallbackCorreo")
    public void sendOrderShipped(UUID idOrden, UUID idTienda) {
        sendOrderEmail(idOrden, "Pedido Enviado",
                "Tu pedido ha sido enviado. Pronto lo recibirás en la dirección indicada.",
                "¡En camino!");
    }

    @CircuitBreaker(name = "correos", fallbackMethod = "fallbackCorreo")
    public void sendOrderDelivered(UUID idOrden, UUID idTienda) {
        sendOrderEmail(idOrden, "Pedido Entregado",
                "Tu pedido ha sido entregado exitosamente.",
                "¡Esperamos que disfrutes tu compra!");
    }

    @CircuitBreaker(name = "correos", fallbackMethod = "fallbackCorreo")
    public void sendOrderCancelled(UUID idOrden, UUID idTienda) {
        sendOrderEmail(idOrden, "Pedido Cancelado",
                "Tu pedido ha sido cancelado. Si tienes preguntas, por favor contáctanos.",
                "Orden Cancelada");
    }

    @CircuitBreaker(name = "correos", fallbackMethod = "fallbackCorreo")
    public void sendOrderRefunded(UUID idOrden, UUID idTienda) {
        sendOrderEmail(idOrden, "Reembolso Procesado",
                "El reembolso de tu pedido ha sido procesado. El monto será devuelto a tu método de pago original en los próximos días.",
                "Reembolso Exitoso");
    }

    private void sendOrderEmail(UUID idOrden, String subject, String bodyMessage, String headline) {
        ResumenOrden orden = servicioConsultaOrden.obtenerResumen(idOrden);
        try {
            Context context = contextoBase(headline, bodyMessage, orden.numeroOrden(), orden.nombreCliente(), orden.totalFormateado());
            MimeMessage mimeMessage = armarMensaje(subject, orden.numeroOrden(), orden.correoCliente(), context);
            mailSender.send(mimeMessage);
            log.info("Correo HTML enviado a {} para la orden {}: {}", orden.correoCliente(), orden.numeroOrden(), subject);
        } catch (Exception e) {
            log.error("Fallo al enviar correo HTML para la orden {}: {}", idOrden, e.getMessage(), e);
            throw new RuntimeException("Error en SMTP", e); // Relanzar para que el Circuit Breaker lo detecte
        }
    }

    private void enviarConAdjunto(FacturaOrden factura, String subject, String bodyMessage, String headline,
                                  String attachmentName, byte[] attachmentData, String contentType) throws Exception {
        String total = factura.getTotal() != null
                ? factura.getTotal().getMonto() + " " + factura.getTotal().getMoneda() : "";
        Context context = contextoBase(headline, bodyMessage, factura.getNumeroOrden(), factura.getNombreCliente(), total);

        MimeMessage mimeMessage = armarMensaje(subject, factura.getNumeroOrden(), factura.getCorreoCliente(), context);
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
        helper.addAttachment(attachmentName, new org.springframework.core.io.ByteArrayResource(attachmentData), contentType);

        mailSender.send(mimeMessage);
        log.info("Correo HTML con adjunto enviado a {} para la orden {}: {}",
                factura.getCorreoCliente(), factura.getNumeroOrden(), subject);
    }

    private Context contextoBase(String headline, String bodyMessage, String numeroOrden, String nombreCliente, String totalFormateado) {
        Context context = new Context();
        context.setVariable("headline", headline);
        context.setVariable("messageBody", bodyMessage);
        context.setVariable("orderNumber", numeroOrden);
        context.setVariable("customerName", nombreCliente);
        context.setVariable("totalAmount", totalFormateado);
        return context;
    }

    private MimeMessage armarMensaje(String subject, String numeroOrden, String destinatario, Context context) throws Exception {
        String cuerpo = templateEngine.process("email/plantilla-orden", context);
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
        helper.setTo(destinatario);
        helper.setSubject(subject + " - #" + numeroOrden);
        helper.setText(cuerpo, true);
        helper.setFrom("noreply@nexasaas.com");
        return mimeMessage;
    }

    // Fallback genérico para correos
    public void fallbackCorreo(UUID idOrden, UUID idTienda, Throwable t) {
        log.warn("CIRCUIT BREAKER ABIERTO para correos. Fallback ejecutado para la orden: {}. El correo no se envió.", idOrden);
        // Aquí podríamos guardar el correo en una tabla de "Correos Pendientes" para reintentar más tarde.
    }
}
