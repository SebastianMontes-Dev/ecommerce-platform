package com.ecommerce.modulos.pagos.application;

import com.ecommerce.modulos.ordenes.domain.Orden;
import com.ecommerce.modulos.ordenes.domain.ArticuloOrden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServicioFacturaPdf {

    private final TemplateEngine templateEngine;

    public byte[] generarPdf(Orden orden) {
        try {
            Context context = new Context();
            context.setVariable("orden", orden);
            
            // Render HTML using Thymeleaf
            String html = templateEngine.process("pdf/factura", context);

            // Generate PDF from HTML
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(outputStream);
            
            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error("Error al generar PDF de factura para la orden {}", orden.getId(), e);
            throw new RuntimeException("Error al generar PDF de factura", e);
        }
    }
}
