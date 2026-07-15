package com.ecommerce.modules.catalog.domain.events;

import com.ecommerce.modules.shared.domain.DomainEvent;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class ProductCreatedEvent implements DomainEvent {

    private final UUID idEvento;
    private final Instant ocurrioEn;
    private final UUID idProducto;
    private final UUID idTienda;
    private final String nombre;
    private final String slug;
    private final String descripcion;
    private final String estado;

    public ProductCreatedEvent(UUID idProducto, UUID idTienda, String nombre, String slug, String descripcion, String estado) {
        this.idEvento = UUID.randomUUID();
        this.ocurrioEn = Instant.now();
        this.idProducto = idProducto;
        this.idTienda = idTienda;
        this.nombre = nombre;
        this.slug = slug;
        this.descripcion = descripcion;
        this.estado = estado;
    }

    @Override
    public String getTipoEvento() {
        return "PRODUCT_CREATED";
    }
}
