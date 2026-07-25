package com.ecommerce.modulos.busqueda.application;

import com.ecommerce.modulos.catalogo.domain.eventos.EventoProductoCreado;
import com.ecommerce.modulos.catalogo.domain.eventos.EventoProductoActualizado;
import com.ecommerce.modulos.catalogo.domain.eventos.EventoProductoEliminado;
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
        indexarDocumento(
                evento.getIdProducto().toString(),
                evento.getIdTienda(),
                evento.getNombre(),
                evento.getEnlaceCorto(),
                evento.getDescripcion(),
                evento.getPrecio(),
                evento.getNombreCategoria()
        );
    }

    @Async
    @EventListener
    public void onProductUpdated(EventoProductoActualizado evento) {
        log.info("Received EventoProductoActualizado for producto: {}", evento.getIdProducto());
        indexarDocumento(
                evento.getIdProducto().toString(),
                evento.getIdTienda(),
                evento.getNombre(),
                evento.getEnlaceCorto(),
                evento.getDescripcion(),
                evento.getPrecio(),
                evento.getNombreCategoria()
        );
    }

    @Async
    @EventListener
    public void onProductDeleted(EventoProductoEliminado evento) {
        log.info("Received EventoProductoEliminado for producto: {}", evento.getIdProducto());
        servicioBusqueda.deleteProduct(evento.getIdTienda(), evento.getIdProducto().toString());
    }

    private void indexarDocumento(String id, java.util.UUID idTienda, String nombre, String enlaceCorto, String descripcion, BigDecimal precio, String nombreCategoria) {
        DocumentoProducto doc = new DocumentoProducto();
        doc.setId(id);
        doc.setIdTienda(idTienda);
        doc.setNombre(nombre);
        doc.setEnlaceCorto(enlaceCorto);
        doc.setDescripcion(descripcion);
        doc.setPrecio(precio);
        doc.setNombreCategoria(nombreCategoria);
        
        servicioBusqueda.indexProduct(doc);
    }
}
