package com.ecommerce.modulos.ordenes.domain.events;

import com.ecommerce.modulos.compartido.domain.EventoDominio;
import com.ecommerce.modulos.ordenes.domain.ArticuloOrden;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Se emite cuando el inventario reservado por una orden debe devolverse al stock: cuando la
 * orden pasa a {@code CANCELLED} (pago expirado/rechazado, o cancelación del cliente) o a
 * {@code REFUNDED}. Lleva los items (payload autónomo) para que el módulo de catálogo reponga
 * el stock sin consultar la base de datos de órdenes. Es el evento simétrico a
 * {@link EventoOrdenCreada}.
 */
@Getter
public class EventoInventarioLiberado implements EventoDominio {

    private final UUID idEvento;
    private final Instant ocurrioEn;

    private final UUID idOrden;
    private final UUID idTienda;
    private final String motivo;
    private final List<ItemInfo> items;

    public EventoInventarioLiberado(UUID idOrden, UUID idTienda, String motivo, List<ArticuloOrden> articulos) {
        this.idEvento = UUID.randomUUID();
        this.ocurrioEn = Instant.now();
        this.idOrden = idOrden;
        this.idTienda = idTienda;
        this.motivo = motivo;
        this.items = articulos.stream()
                .map(a -> new ItemInfo(a.getIdProducto(), a.getVariantId(), a.getCantidad()))
                .collect(Collectors.toList());
    }

    @Override
    public String getTipoEvento() {
        return "ORDER_INVENTORY_RELEASED";
    }

    @Getter
    public static class ItemInfo {
        private final UUID idProducto;
        private final UUID variantId;
        private final int cantidad;

        public ItemInfo(UUID idProducto, UUID variantId, int cantidad) {
            this.idProducto = idProducto;
            this.variantId = variantId;
            this.cantidad = cantidad;
        }
    }
}
