package com.ecommerce.modulos.notificacion.application;

import com.ecommerce.modulos.ordenes.application.dto.FacturaOrden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;

/**
 * Renderiza la factura de una orden a PDF (HTML Thymeleaf → PDF con flying-saucer).
 *
 * <p>Vive en <b>notificacion</b> porque la factura es un adjunto del correo de "pago
 * recibido"; recibe un {@link FacturaOrden} (DTO del módulo ordenes), no la entidad.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ServicioFacturaPdf {

    private final TemplateEngine templateEngine;

    public byte[] generarPdf(FacturaOrden factura) {
        try {
            Context context = new Context();
            context.setVariable("orden", factura); // la plantilla pdf/factura usa ${orden.*}

            String html = templateEngine.process("pdf/factura", context);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error("Error al generar PDF de factura para la orden {}", factura.getNumeroOrden(), e);
            throw new RuntimeException("Error al generar PDF de factura", e);
        }
    }
}
