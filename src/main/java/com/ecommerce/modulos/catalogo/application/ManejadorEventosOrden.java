package com.ecommerce.modulos.catalogo.application;

import com.ecommerce.modulos.catalogo.domain.Producto;
import com.ecommerce.modulos.catalogo.domain.RepositorioProducto;
import com.ecommerce.modulos.catalogo.domain.RepositorioVarianteProducto;
import com.ecommerce.modulos.catalogo.domain.VarianteProducto;
import com.ecommerce.modulos.ordenes.domain.events.EventoOrdenCancelada;
import com.ecommerce.modulos.ordenes.domain.events.EventoOrdenCreada;
import com.ecommerce.modulos.compartido.domain.ExcepcionEntidadNoEncontrada;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Ajusta el inventario en respuesta a eventos de orden. Corre <b>síncrono y dentro de la
 * transacción</b> que publicó el evento: si la reserva falla por stock insuficiente, la orden
 * entera hace rollback.
 *
 * <p>La consistencia bajo concurrencia se garantiza con bloqueo pesimista
 * ({@code findByIdForUpdate} → {@code SELECT ... FOR UPDATE}). Los items se procesan ordenados
 * por {@code (idProducto, variantId)} para que dos órdenes que compiten por los mismos
 * productos adquieran los locks en el mismo orden y no haya deadlock.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ManejadorEventosOrden {

    private final RepositorioProducto repositorioProducto;
    private final RepositorioVarianteProducto repositorioVarianteProducto;

    @EventListener
    @Transactional
    public void handle(EventoOrdenCreada event) {
        log.info("Reservando inventario para orden {} (Inquilino: {})", event.getIdOrden(), event.getIdTienda());

        for (Linea linea : lineasOrdenadas(event.getItems(), i -> new Linea(i.getIdProducto(), i.getVariantId(), i.getCantidad()))) {
            reservar(linea);
        }
    }

    /**
     * Repone el inventario cuando una orden se cancela (pago expirado, rechazado o cancelación
     * explícita). Manejador simétrico a {@link #handle(EventoOrdenCreada)}.
     */
    @EventListener
    @Transactional
    public void handle(EventoOrdenCancelada event) {
        log.info("Reponiendo inventario por cancelación de orden {} (Inquilino: {}). Motivo: {}",
                event.getIdOrden(), event.getIdTienda(), event.getMotivo());

        for (Linea linea : lineasOrdenadas(event.getItems(), i -> new Linea(i.getIdProducto(), i.getVariantId(), i.getCantidad()))) {
            reponer(linea);
        }
    }

    private void reservar(Linea linea) {
        if (linea.variantId() != null) {
            VarianteProducto variante = bloquearVariante(linea.variantId());
            variante.decreaseInventory(linea.cantidad());
            repositorioVarianteProducto.save(variante);
            log.info("Variante {}: reservadas {} uds. Stock: {}", variante.getNombre(), linea.cantidad(), variante.getInventario());
        } else {
            Producto producto = bloquearProducto(linea.idProducto());
            producto.decreaseInventory(linea.cantidad());
            repositorioProducto.save(producto);
            log.info("Producto {}: reservadas {} uds. Stock: {}", producto.getNombre(), linea.cantidad(), producto.getInventario());
        }
    }

    private void reponer(Linea linea) {
        if (linea.variantId() != null) {
            VarianteProducto variante = bloquearVariante(linea.variantId());
            variante.increaseInventory(linea.cantidad());
            repositorioVarianteProducto.save(variante);
            log.info("Variante {}: repuestas {} uds. Stock: {}", variante.getNombre(), linea.cantidad(), variante.getInventario());
        } else {
            Producto producto = bloquearProducto(linea.idProducto());
            producto.increaseInventory(linea.cantidad());
            repositorioProducto.save(producto);
            log.info("Producto {}: repuestas {} uds. Stock: {}", producto.getNombre(), linea.cantidad(), producto.getInventario());
        }
    }

    private Producto bloquearProducto(UUID id) {
        return repositorioProducto.findByIdForUpdate(id)
                .orElseThrow(() -> new ExcepcionEntidadNoEncontrada("Producto", id));
    }

    private VarianteProducto bloquearVariante(UUID id) {
        return repositorioVarianteProducto.findByIdForUpdate(id)
                .orElseThrow(() -> new ExcepcionEntidadNoEncontrada("VarianteProducto", id));
    }

    private static <T> List<Linea> lineasOrdenadas(List<T> items, java.util.function.Function<T, Linea> aLinea) {
        return items.stream()
                .map(aLinea)
                .sorted(Comparator.comparing((Linea l) -> l.idProducto().toString())
                        .thenComparing(l -> String.valueOf(l.variantId())))
                .toList();
    }

    private record Linea(UUID idProducto, UUID variantId, int cantidad) {}
}
