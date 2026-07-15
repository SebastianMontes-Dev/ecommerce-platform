package com.ecommerce.modulos.catalogo.domain.eventos;

import com.ecommerce.modulos.compartido.domain.EventoDominio;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class EventoProductoCreado implements EventoDominio {

    private final UUID idEvento;
    private final Instant ocurrioEn;
    private final UUID idProducto;
    private final UUID idTienda;
    private final String nombre;
    private final String enlaceCorto;
    private final String descripcion;
    private final String estado;

    public EventoProductoCreado(UUID idProducto, UUID idTienda, String nombre, String enlaceCorto, String descripcion, String estado) {
        this.idEvento = UUID.randomUUID();
        this.ocurrioEn = Instant.now();
        this.idProducto = idProducto;
        this.idTienda = idTienda;
        this.nombre = nombre;
        this.enlaceCorto = enlaceCorto;
        this.descripcion = descripcion;
        this.estado = estado;
    }

    @Override
    public String getTipoEvento() {
        return "PRODUCT_CREATED";
    }
}
