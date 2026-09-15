package com.ecommerce.modulos.carrito.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ArticuloCarritoTest {

    @Test
    @DisplayName("getSubtotal multiplica el precio unitario por la cantidad")
    void getSubtotalMultiplicaPrecioPorCantidad() {
        ArticuloCarrito item = ArticuloCarrito.builder()
                .idProducto(UUID.randomUUID())
                .cantidad(3)
                .precioUnitario(new BigDecimal("9.90"))
                .build();

        assertEquals(new BigDecimal("29.70"), item.getSubtotal());
    }

    @Test
    @DisplayName("getSubtotal es cero cuando la cantidad es cero")
    void getSubtotalConCantidadCeroEsCero() {
        ArticuloCarrito item = ArticuloCarrito.builder()
                .idProducto(UUID.randomUUID())
                .cantidad(0)
                .precioUnitario(new BigDecimal("9.90"))
                .build();

        assertEquals(new BigDecimal("0.00"), item.getSubtotal());
    }

    @Test
    @DisplayName("dos articulos con los mismos campos son iguales y comparten hashCode (Lombok @Data)")
    void equalsYHashCodeConsistentesEntreInstanciasIguales() {
        UUID idProducto = UUID.randomUUID();
        ArticuloCarrito uno = ArticuloCarrito.builder()
                .idProducto(idProducto)
                .cantidad(2)
                .precioUnitario(new BigDecimal("5.00"))
                .moneda("USD")
                .build();
        ArticuloCarrito otro = ArticuloCarrito.builder()
                .idProducto(idProducto)
                .cantidad(2)
                .precioUnitario(new BigDecimal("5.00"))
                .moneda("USD")
                .build();

        assertEquals(uno, otro);
        assertEquals(uno.hashCode(), otro.hashCode());
    }

    @Test
    @DisplayName("dos articulos con distinta cantidad no son iguales")
    void equalsFalseCuandoDifiereLaCantidad() {
        UUID idProducto = UUID.randomUUID();
        ArticuloCarrito uno = ArticuloCarrito.builder()
                .idProducto(idProducto)
                .cantidad(1)
                .precioUnitario(new BigDecimal("5.00"))
                .build();
        ArticuloCarrito otro = ArticuloCarrito.builder()
                .idProducto(idProducto)
                .cantidad(2)
                .precioUnitario(new BigDecimal("5.00"))
                .build();

        assertNotEquals(uno, otro);
    }
}
