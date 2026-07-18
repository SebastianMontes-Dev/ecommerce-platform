package com.ecommerce.modulos.ordenes.domain.events;

import com.ecommerce.modulos.compartido.domain.EventoDominio;
import com.ecommerce.modulos.ordenes.domain.ArticuloOrden;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
public class EventoOrdenCreada implements EventoDominio {
    private final UUID idEvento;
    private final Instant ocurrioEn;
    
    private final UUID idOrden;
    private final UUID idTienda;
    private final UUID idCliente;
    private final List<ItemInfo> items;

    public EventoOrdenCreada(UUID idOrden, UUID idTienda, UUID idCliente, List<ArticuloOrden> articulos) {
        this.idEvento = UUID.randomUUID();
        this.ocurrioEn = Instant.now();
        this.idOrden = idOrden;
        this.idTienda = idTienda;
        this.idCliente = idCliente;
        this.items = articulos.stream()
                .map(a -> new ItemInfo(a.getIdProducto(), a.getCantidad()))
                .collect(Collectors.toList());
    }

    @Override
    public String getTipoEvento() {
        return "ORDER_CREATED";
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
