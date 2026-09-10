package com.ecommerce.modulos.notificacion.application;

import com.ecommerce.modulos.compartido.domain.Dinero;
import com.ecommerce.modulos.compartido.domain.ExcepcionEntidadNoEncontrada;
import com.ecommerce.modulos.ordenes.application.ServicioConsultaOrden;
import com.ecommerce.modulos.ordenes.application.ServicioConsultaOrden.ResumenOrden;
import com.ecommerce.modulos.ordenes.application.dto.FacturaOrden;
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
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServicioNotificacionCorreoTest {

    @Mock private JavaMailSender mailSender;
    @Mock private TemplateEngine templateEngine;
    @Mock private ServicioConsultaOrden servicioConsultaOrden;
    @Mock private ServicioFacturaPdf servicioFacturaPdf;

    @InjectMocks
    private ServicioNotificacionCorreo servicioNotificacionCorreo;

    private UUID idOrden;
    private UUID idTienda;

    @BeforeEach
    void setUp() {
        idOrden = UUID.randomUUID();
        idTienda = UUID.randomUUID();
    }

    private MimeMessage mimeMessageReal() {
        return new MimeMessage(Session.getInstance(new Properties()));
    }

    private ResumenOrden resumen(String nombreCliente) {
        return new ResumenOrden("ORD-1001", "cliente@test.com", nombreCliente, "150.00 USD");
    }

    private FacturaOrden facturaMinima() {
        return FacturaOrden.builder()
                .numeroOrden("ORD-1001")
                .nombreCliente("Ana Pérez")
                .correoCliente("cliente@test.com")
                .articulos(List.of())
                .total(Dinero.of(new BigDecimal("150.00"), "USD"))
                .build();
    }

    @Test
    void debeEnviarCorreoDeConfirmacionConDatosDelResumen() throws Exception {
        when(servicioConsultaOrden.obtenerResumen(idOrden)).thenReturn(resumen("Ana Pérez"));
        when(templateEngine.process(eq("email/plantilla-orden"), any(Context.class))).thenReturn("<html>ok</html>");
        MimeMessage mimeMessage = mimeMessageReal();
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        servicioNotificacionCorreo.sendOrderConfirmation(idOrden, idTienda);

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq("email/plantilla-orden"), contextCaptor.capture());
        Context context = contextCaptor.getValue();
        assertEquals("¡Gracias por tu compra!", context.getVariable("headline"));
        assertEquals("ORD-1001", context.getVariable("orderNumber"));
        assertEquals("Ana Pérez", context.getVariable("customerName"));
        assertEquals("150.00 USD", context.getVariable("totalAmount"));

        verify(mailSender).send(mimeMessage);
        assertEquals("Pedido Confirmado - #ORD-1001", mimeMessage.getSubject());
        assertEquals("cliente@test.com", mimeMessage.getAllRecipients()[0].toString());
    }

    @Test
    void debePasarElNombreDeClienteQueDaElResumen() throws Exception {
        when(servicioConsultaOrden.obtenerResumen(idOrden)).thenReturn(resumen("Cliente"));
        when(templateEngine.process(eq("email/plantilla-orden"), any(Context.class))).thenReturn("<html>ok</html>");
        when(mailSender.createMimeMessage()).thenReturn(mimeMessageReal());

        servicioNotificacionCorreo.sendOrderShipped(idOrden, idTienda);

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq("email/plantilla-orden"), contextCaptor.capture());
        assertEquals("Cliente", contextCaptor.getValue().getVariable("customerName"));
    }

    @Test
    void debePropagarExcepcionEntidadNoEncontradaSinEnvolver() {
        when(servicioConsultaOrden.obtenerResumen(idOrden))
                .thenThrow(new ExcepcionEntidadNoEncontrada("Orden", idOrden));

        assertThrows(ExcepcionEntidadNoEncontrada.class,
                () -> servicioNotificacionCorreo.sendOrderConfirmation(idOrden, idTienda));

        verifyNoInteractions(mailSender);
    }

    @Test
    void debeLanzarRuntimeExceptionSiJavaMailSenderFalla() {
        when(servicioConsultaOrden.obtenerResumen(idOrden)).thenReturn(resumen("Ana Pérez"));
        when(templateEngine.process(eq("email/plantilla-orden"), any(Context.class))).thenReturn("<html>ok</html>");
        when(mailSender.createMimeMessage()).thenReturn(mimeMessageReal());
        doThrow(new MailSendException("SMTP caído")).when(mailSender).send(any(MimeMessage.class));

        RuntimeException excepcion = assertThrows(RuntimeException.class,
                () -> servicioNotificacionCorreo.sendOrderDelivered(idOrden, idTienda));

        assertEquals("Error en SMTP", excepcion.getMessage());
        assertInstanceOf(MailSendException.class, excepcion.getCause());
    }

    @Test
    void debeEnviarCorreoDeCancelacionConSubjectYHeadlineCorrectos() throws Exception {
        when(servicioConsultaOrden.obtenerResumen(idOrden)).thenReturn(resumen("Ana Pérez"));
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
    void debeEnviarCorreoDePagoRecibidoConFacturaAdjunta() throws Exception {
        FacturaOrden factura = facturaMinima();
        when(servicioConsultaOrden.obtenerFactura(idOrden)).thenReturn(factura);
        when(templateEngine.process(eq("email/plantilla-orden"), any(Context.class))).thenReturn("<html>ok</html>");
        when(servicioFacturaPdf.generarPdf(factura)).thenReturn("PDF-CONTENT".getBytes());
        MimeMessage mimeMessage = mimeMessageReal();
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        servicioNotificacionCorreo.sendPaymentReceived(idOrden, idTienda);

        verify(servicioFacturaPdf).generarPdf(factura);
        verify(mailSender).send(mimeMessage);
        assertEquals("Pago Recibido - #ORD-1001", mimeMessage.getSubject());

        Object content = mimeMessage.getContent();
        assertInstanceOf(jakarta.mail.Multipart.class, content);
        assertEquals(2, ((jakarta.mail.Multipart) content).getCount());
    }

    @Test
    void debeLanzarRuntimeExceptionSiGeneracionDePdfFalla() {
        FacturaOrden factura = facturaMinima();
        when(servicioConsultaOrden.obtenerFactura(idOrden)).thenReturn(factura);
        when(servicioFacturaPdf.generarPdf(factura)).thenThrow(new RuntimeException("Error al generar PDF de factura"));

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
