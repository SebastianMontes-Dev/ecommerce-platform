package com.ecommerce.modulos.busqueda.application;

import com.ecommerce.modulos.catalogo.domain.eventos.EventoProductoCreado;
import com.ecommerce.modulos.busqueda.domain.DocumentoProducto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class ConsumidorEventoBusqueda {

    private final ServicioBusqueda servicioBusqueda;

    @Async
    @EventListener
    public void onProductCreated(EventoProductoCreado evento) {
        log.info("Received EventoProductoCreado for producto: {}", evento.getIdProducto());
        
        DocumentoProducto doc = new DocumentoProducto();
        doc.setId(evento.getIdProducto().toString());
        doc.setIdTienda(evento.getIdTienda());
        doc.setNombre(evento.getNombre());
        doc.setEnlaceCorto(evento.getEnlaceCorto());
        doc.setDescripcion(evento.getDescripcion());
        doc.setPrecio(BigDecimal.ZERO); // Normally comes from evento or API
        
        servicioBusqueda.indexProduct(doc);
    }
}
