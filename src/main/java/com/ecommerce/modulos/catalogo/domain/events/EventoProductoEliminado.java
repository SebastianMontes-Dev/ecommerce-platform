package com.ecommerce.modulos.catalogo.domain.eventos;

import com.ecommerce.modulos.compartido.domain.EventoDominio;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class EventoProductoEliminado implements EventoDominio {

    private final UUID idEvento;
    private final Instant ocurrioEn;
    private final UUID idProducto;
    private final UUID idTienda;

    public EventoProductoEliminado(UUID idProducto, UUID idTienda) {
        this.idEvento = UUID.randomUUID();
        this.ocurrioEn = Instant.now();
        this.idProducto = idProducto;
        this.idTienda = idTienda;
    }

    @Override
    public String getTipoEvento() {
        return "PRODUCT_DELETED";
    }
}
