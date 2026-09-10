package com.ecommerce.modulos.ordenes.domain.events;

import com.ecommerce.modulos.compartido.domain.EventoDominio;
import com.ecommerce.modulos.ordenes.domain.ArticuloOrden;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Se emite cuando una orden pasa a CANCELLED. Lleva los items (payload autónomo) para
 * que el módulo de catálogo pueda reponer el inventario reservado sin tener que
 * consultar la base de datos de órdenes. Es el evento simétrico a {@link EventoOrdenCreada}.
 */
@Getter
public class EventoOrdenCancelada implements EventoDominio {

    private final UUID idEvento;
    private final Instant ocurrioEn;

    private final UUID idOrden;
    private final UUID idTienda;
    private final String motivo;
    private final List<ItemInfo> items;

    public EventoOrdenCancelada(UUID idOrden, UUID idTienda, String motivo, List<ArticuloOrden> articulos) {
        this.idEvento = UUID.randomUUID();
        this.ocurrioEn = Instant.now();
        this.idOrden = idOrden;
        this.idTienda = idTienda;
        this.motivo = motivo;
        this.items = articulos.stream()
                .map(a -> new ItemInfo(a.getIdProducto(), a.getCantidad()))
                .collect(Collectors.toList());
    }

    @Override
    public String getTipoEvento() {
        return "ORDER_CANCELLED";
    }

    @Getter
    public static class ItemInfo {
        private final UUID idProducto;
        private final int cantidad;

        public ItemInfo(UUID idProducto, int cantidad) {
            this.idProducto = idProducto;
            this.cantidad = cantidad;
        }
    }
}
