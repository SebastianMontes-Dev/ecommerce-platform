package com.ecommerce.modulos.compartido.domain;

/**
 * El inventario disponible no alcanza para la cantidad pedida (y el producto no admite
 * reserva/backorder). Se traduce a HTTP 409 Conflict: el estado del recurso cambió
 * (se agotó el stock) respecto a lo que el cliente asumía al armar el carrito.
 */
public class ExcepcionStockInsuficiente extends ExcepcionOperacionInvalida {

    public ExcepcionStockInsuficiente(String detalle) {
        super(detalle);
    }
}
