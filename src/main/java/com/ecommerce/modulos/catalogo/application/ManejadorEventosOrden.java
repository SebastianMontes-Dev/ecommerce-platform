package com.ecommerce.modulos.catalogo.application;

import com.ecommerce.modulos.catalogo.domain.Producto;
import com.ecommerce.modulos.catalogo.domain.RepositorioProducto;
import com.ecommerce.modulos.ordenes.domain.events.EventoOrdenCreada;
import com.ecommerce.modulos.compartido.domain.ExcepcionEntidadNoEncontrada;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManejadorEventosOrden {

    private final RepositorioProducto repositorioProducto;

    @EventListener
    @Transactional
    public void handle(EventoOrdenCreada event) {
        log.info("Procesando reserva de inventario para orden {} (Inquilino: {})", event.getIdOrden(), event.getIdTienda());

        for (EventoOrdenCreada.ItemInfo item : event.getItems()) {
            Producto producto = repositorioProducto.findById(item.getIdProducto())
                    .orElseThrow(() -> new ExcepcionEntidadNoEncontrada("Producto", item.getIdProducto()));

            producto.decreaseInventory(item.getCantidad());
            repositorioProducto.save(producto);
            
            log.info("Inventario reducido en {} para producto {}. Nuevo stock: {}", 
                item.getCantidad(), producto.getNombre(), producto.getInventario());
        }
    }
}
