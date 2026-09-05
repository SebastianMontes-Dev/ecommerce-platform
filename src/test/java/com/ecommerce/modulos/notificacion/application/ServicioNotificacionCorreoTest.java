package com.ecommerce.modulos.notificacion.application;

import com.ecommerce.modulos.compartido.domain.Dinero;
import com.ecommerce.modulos.compartido.domain.ExcepcionEntidadNoEncontrada;
import com.ecommerce.modulos.ordenes.domain.Orden;
import com.ecommerce.modulos.ordenes.domain.RepositorioOrden;
import com.ecommerce.modulos.pagos.application.ServicioFacturaPdf;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServicioNotificacionCorreoTest {

    @Mock
    private JavaMailSender mailSender;
    @Mock
    private RepositorioOrden repositorioOrden;
    @Mock
    private TemplateEngine templateEngine;
    @Mock
    private ServicioFacturaPdf servicioFacturaPdf;

    @InjectMocks
    private ServicioNotificacionCorreo servicioNotificacionCorreo;

    private UUID idOrden;
    private UUID idTienda;
    private Orden orden;

    @BeforeEach
    void setUp() {
        idOrden = UUID.randomUUID();
        idTienda = UUID.randomUUID();
        orden = new Orden();
        orden.setNumeroOrden("ORD-1001");
        orden.setCorreoCliente("cliente@test.com");
        orden.setNombreCliente("Ana Pérez");
        orden.setTotal(Dinero.of(new BigDecimal("150.00"), "USD"));
    }

    private MimeMessage mimeMessageReal() {
        return new MimeMessage(Session.getInstance(new Properties()));
    }

    @Test
    void debeEnviarCorreoDeConfirmacionConDatosCorrectosDeLaOrden() throws Exception {
        when(repositorioOrden.findById(idOrden)).thenReturn(Optional.of(orden));
        when(templateEngine.process(eq("email/plantilla-orden"), any(Context.class))).thenReturn("<html>ok</html>");
        MimeMessage mimeMessage = mimeMessageReal();
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        servicioNotificacionCorreo.sendOrderConfirmation(idOrden, idTienda);

        // El cuerpo se arma vía Thymeleaf con las variables correctas
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq("email/plantilla-orden"), contextCaptor.capture());
        Context context = contextCaptor.getValue();
        assertEquals("¡Gracias por tu compra!", context.getVariable("headline"));
        assertEquals("Tu pedido ha sido confirmado y está siendo procesado.", context.getVariable("messageBody"));
        assertEquals("ORD-1001", context.getVariable("orderNumber"));
        assertEquals("Ana Pérez", context.getVariable("customerName"));
        assertEquals("150.00 USD", context.getVariable("totalAmount"));

        // El mensaje se envía con destinatario y asunto correctos
        verify(mailSender).send(mimeMessage);
        assertEquals("Pedido Confirmado - #ORD-1001", mimeMessage.getSubject());
        assertEquals("cliente@test.com", mimeMessage.getAllRecipients()[0].toString());
    }

    @Test
    void debeUsarClienteComoNombrePorDefectoCuandoNombreClienteEsNulo() throws Exception {
        orden.setNombreCliente(null);
        when(repositorioOrden.findById(idOrden)).thenReturn(Optional.of(orden));
        when(templateEngine.process(eq("email/plantilla-orden"), any(Context.class))).thenReturn("<html>ok</html>");
        when(mailSender.createMimeMessage()).thenReturn(mimeMessageReal());

        servicioNotificacionCorreo.sendOrderShipped(idOrden, idTienda);

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq("email/plantilla-orden"), contextCaptor.capture());
        assertEquals("Cliente", contextCaptor.getValue().getVariable("customerName"));
    }

    @Test
    void debePropagarExcepcionEntidadNoEncontradaSinEnvolverSiOrdenNoExiste() {
        when(repositorioOrden.findById(idOrden)).thenReturn(Optional.empty());

        // La excepción de dominio (orden no encontrada) se propaga tal cual, sin envolverse
        // en el RuntimeException genérico de fallas de SMTP - el llamador (y el circuit breaker,
        // vía resilience4j.circuitbreaker.instances.correos.ignoreExceptions) puede distinguirla
        // de una falla real de infraestructura.
        assertThrows(ExcepcionEntidadNoEncontrada.class,
                () -> servicioNotificacionCorreo.sendOrderConfirmation(idOrden, idTienda));

        verifyNoInteractions(mailSender);
    }

    @Test
    void debeLanzarRuntimeExceptionSiJavaMailSenderFalla() {
        when(repositorioOrden.findById(idOrden)).thenReturn(Optional.of(orden));
        when(templateEngine.process(eq("email/plantilla-orden"), any(Context.class))).thenReturn("<html>ok</html>");
        when(mailSender.createMimeMessage()).thenReturn(mimeMessageReal());
        doThrow(new MailSendException("SMTP caído"))
                .when(mailSender).send(any(MimeMessage.class));

        RuntimeException excepcion = assertThrows(RuntimeException.class,
                () -> servicioNotificacionCorreo.sendOrderDelivered(idOrden, idTienda));

        assertEquals("Error en SMTP", excepcion.getMessage());
        assertInstanceOf(MailSendException.class, excepcion.getCause());
    }

    @Test
    void debeEnviarCorreoDeCancelacionConSubjectYHeadlineCorrectos() throws Exception {
        when(repositorioOrden.findById(idOrden)).thenReturn(Optional.of(orden));
        when(templateEngine.process(eq("email/plantilla-orden"), any(Context.class))).thenReturn("<html>ok</html>");
        MimeMessage mimeMessage = mimeMessageReal();
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        servicioNotificacionCorreo.sendOrderCancelled(idOrden, idTienda);

        assertEquals("Pedido Cancelado - #ORD-1001", mimeMessage.getSubject());
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq("email/plantilla-orden"), contextCaptor.capture());
        assertEquals("Orden Cancelada", contextCaptor.getValue().getVariable("headline"));
    }

    @Test
    void debeEnviarCorreoDeReembolsoConSubjectYHeadlineCorrectos() throws Exception {
        when(repositorioOrden.findById(idOrden)).thenReturn(Optional.of(orden));
        when(templateEngine.process(eq("email/plantilla-orden"), any(Context.class))).thenReturn("<html>ok</html>");
        MimeMessage mimeMessage = mimeMessageReal();
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        servicioNotificacionCorreo.sendOrderRefunded(idOrden, idTienda);

        assertEquals("Reembolso Procesado - #ORD-1001", mimeMessage.getSubject());
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq("email/plantilla-orden"), contextCaptor.capture());
        assertEquals("Reembolso Exitoso", contextCaptor.getValue().getVariable("headline"));
    }

    @Test
    void debeEnviarCorreoDePagoRecibidoConFacturaAdjunta() throws Exception {
        when(repositorioOrden.findById(idOrden)).thenReturn(Optional.of(orden));
        when(templateEngine.process(eq("email/plantilla-orden"), any(Context.class))).thenReturn("<html>ok</html>");
        byte[] pdf = "PDF-CONTENT".getBytes();
        when(servicioFacturaPdf.generarPdf(orden)).thenReturn(pdf);
        MimeMessage mimeMessage = mimeMessageReal();
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        servicioNotificacionCorreo.sendPaymentReceived(idOrden, idTienda);

        verify(servicioFacturaPdf).generarPdf(orden);
        verify(mailSender).send(mimeMessage);
        assertEquals("Pago Recibido - #ORD-1001", mimeMessage.getSubject());

        // El mensaje queda armado como multipart: cuerpo HTML + adjunto PDF
        Object content = mimeMessage.getContent();
        assertInstanceOf(jakarta.mail.Multipart.class, content);
        assertEquals(2, ((jakarta.mail.Multipart) content).getCount());
    }

    @Test
    void debeLanzarRuntimeExceptionSiGeneracionDePdfFalla() {
        when(repositorioOrden.findById(idOrden)).thenReturn(Optional.of(orden));
        when(servicioFacturaPdf.generarPdf(orden)).thenThrow(new RuntimeException("Error al generar PDF de factura"));

        RuntimeException excepcion = assertThrows(RuntimeException.class,
                () -> servicioNotificacionCorreo.sendPaymentReceived(idOrden, idTienda));

        assertEquals("Error en SMTP o Generacion PDF", excepcion.getMessage());
        verifyNoInteractions(mailSender);
    }

    @Test
    void fallbackCorreoNoLanzaExcepcionYNoEnviaCorreo() {
        assertDoesNotThrow(() ->
                servicioNotificacionCorreo.fallbackCorreo(idOrden, idTienda, new RuntimeException("circuito abierto")));

        verifyNoInteractions(mailSender);
    }
}
