package com.ecommerce.modulos.carrito.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CarritoTest {

    private Carrito carrito;
    private UUID idTienda;

    @BeforeEach
    void setUp() {
        idTienda = UUID.randomUUID();
        carrito = Carrito.builder()
                .id("carrito:1")
                .idTienda(idTienda)
                .build();
    }

    private ArticuloCarrito articulo(UUID idProducto, UUID variantId, int cantidad, String precio) {
        return ArticuloCarrito.builder()
                .idProducto(idProducto)
                .variantId(variantId)
                .cantidad(cantidad)
                .precioUnitario(new BigDecimal(precio))
                .moneda("USD")
                .idTienda(idTienda)
                .build();
    }

    @Test
    @DisplayName("Un carrito recien creado esta vacio")
    void carritoNuevoEstaVacio() {
        assertTrue(carrito.isEmpty());
        assertEquals(0, carrito.getItemCount());
        assertEquals(0, carrito.getDistinctItemCount());
        assertEquals(BigDecimal.ZERO, carrito.calcularSubtotal());
        assertEquals(BigDecimal.ZERO, carrito.getTotal());
    }

    @Test
    @DisplayName("agregarArticulo agrega un producto nuevo cuando no existe en el carrito")
    void agregarArticuloNuevo() {
        ArticuloCarrito item = articulo(UUID.randomUUID(), null, 2, "10.00");

        carrito.agregarArticulo(item);

        assertFalse(carrito.isEmpty());
        assertEquals(1, carrito.getDistinctItemCount());
        assertEquals(2, carrito.getItemCount());
    }

    @Test
    @DisplayName("agregarArticulo suma cantidades cuando el mismo producto y variante ya estan en el carrito")
    void agregarArticuloExistenteAcumulaCantidad() {
        UUID idProducto = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        carrito.agregarArticulo(articulo(idProducto, variantId, 2, "10.00"));

        carrito.agregarArticulo(articulo(idProducto, variantId, 3, "10.00"));

        assertEquals(1, carrito.getDistinctItemCount());
        assertEquals(5, carrito.getItemCount());
    }

    @Test
    @DisplayName("agregarArticulo trata como el mismo item dos entradas sin variante (variantId null en ambas)")
    void agregarArticuloSinVarianteAcumulaCantidad() {
        UUID idProducto = UUID.randomUUID();
        carrito.agregarArticulo(articulo(idProducto, null, 1, "5.00"));

        carrito.agregarArticulo(articulo(idProducto, null, 4, "5.00"));

        assertEquals(1, carrito.getDistinctItemCount());
        assertEquals(5, carrito.getItemCount());
    }

    @Test
    @DisplayName("agregarArticulo trata variantes distintas del mismo producto como items separados")
    void agregarArticuloConVariantesDistintasNoAcumula() {
        UUID idProducto = UUID.randomUUID();
        carrito.agregarArticulo(articulo(idProducto, UUID.randomUUID(), 1, "5.00"));

        carrito.agregarArticulo(articulo(idProducto, UUID.randomUUID(), 1, "5.00"));

        assertEquals(2, carrito.getDistinctItemCount());
        assertEquals(2, carrito.getItemCount());
    }

    @Test
    @DisplayName("agregarArticulo lanza IllegalStateException al superar el limite de 50 articulos distintos")
    void agregarArticuloLanzaExcepcionAlSuperarLimite() {
        for (int i = 0; i < 50; i++) {
            carrito.agregarArticulo(articulo(UUID.randomUUID(), null, 1, "1.00"));
        }
        assertEquals(50, carrito.getDistinctItemCount());

        ArticuloCarrito articulo51 = articulo(UUID.randomUUID(), null, 1, "1.00");

        IllegalStateException excepcion = assertThrows(IllegalStateException.class,
                () -> carrito.agregarArticulo(articulo51));
        assertTrue(excepcion.getMessage().contains("50"));
        assertEquals(50, carrito.getDistinctItemCount());
    }

    @Test
    @DisplayName("removerArticulo quita el item que coincide en producto y variante")
    void removerArticuloPorProductoYVariante() {
        UUID idProducto = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        carrito.agregarArticulo(articulo(idProducto, variantId, 1, "5.00"));

        carrito.removerArticulo(idProducto, variantId);

        assertTrue(carrito.isEmpty());
    }

    @Test
    @DisplayName("removerArticulo con variantId null quita todas las variantes de ese producto")
    void removerArticuloConVariantIdNuloQuitaTodasLasVariantes() {
        UUID idProducto = UUID.randomUUID();
        carrito.agregarArticulo(articulo(idProducto, UUID.randomUUID(), 1, "5.00"));
        carrito.agregarArticulo(articulo(idProducto, UUID.randomUUID(), 1, "5.00"));
        carrito.agregarArticulo(articulo(UUID.randomUUID(), null, 1, "5.00"));

        carrito.removerArticulo(idProducto, null);

        assertEquals(1, carrito.getDistinctItemCount());
    }

    @Test
    @DisplayName("removerArticulo sobre un producto inexistente no lanza excepcion y deja el carrito intacto")
    void removerArticuloInexistenteNoHaceNada() {
        carrito.agregarArticulo(articulo(UUID.randomUUID(), null, 1, "5.00"));

        assertDoesNotThrow(() -> carrito.removerArticulo(UUID.randomUUID(), null));

        assertEquals(1, carrito.getDistinctItemCount());
    }

    @Test
    @DisplayName("updateQuantity actualiza la cantidad de un item existente")
    void updateQuantityActualizaCantidadExistente() {
        UUID idProducto = UUID.randomUUID();
        carrito.agregarArticulo(articulo(idProducto, null, 1, "5.00"));

        carrito.updateQuantity(idProducto, null, 7);

        assertEquals(7, carrito.getItemCount());
    }

    @Test
    @DisplayName("updateQuantity con cantidad cero remueve el item del carrito")
    void updateQuantityConCeroRemueveItem() {
        UUID idProducto = UUID.randomUUID();
        carrito.agregarArticulo(articulo(idProducto, null, 3, "5.00"));

        carrito.updateQuantity(idProducto, null, 0);

        assertTrue(carrito.isEmpty());
    }

    @Test
    @DisplayName("updateQuantity con cantidad negativa remueve el item del carrito")
    void updateQuantityConCantidadNegativaRemueveItem() {
        UUID idProducto = UUID.randomUUID();
        carrito.agregarArticulo(articulo(idProducto, null, 3, "5.00"));

        carrito.updateQuantity(idProducto, null, -1);

        assertTrue(carrito.isEmpty());
    }

    @Test
    @DisplayName("updateQuantity sobre un producto que no esta en el carrito no agrega nada ni lanza excepcion")
    void updateQuantitySobreProductoInexistenteNoHaceNada() {
        assertDoesNotThrow(() -> carrito.updateQuantity(UUID.randomUUID(), null, 5));

        assertTrue(carrito.isEmpty());
    }

    @Test
    @DisplayName("clear() vacia el carrito por completo")
    void clearVaciaElCarrito() {
        carrito.agregarArticulo(articulo(UUID.randomUUID(), null, 2, "5.00"));
        carrito.agregarArticulo(articulo(UUID.randomUUID(), null, 3, "5.00"));

        carrito.clear();

        assertTrue(carrito.isEmpty());
        assertEquals(0, carrito.getItemCount());
        assertEquals(0, carrito.getDistinctItemCount());
    }

    @Test
    @DisplayName("getItemCount suma las cantidades de todos los articulos, getDistinctItemCount cuenta lineas")
    void getItemCountYDistinctItemCountDifierenConCantidadesMayoresAUno() {
        carrito.agregarArticulo(articulo(UUID.randomUUID(), null, 3, "5.00"));
        carrito.agregarArticulo(articulo(UUID.randomUUID(), null, 4, "5.00"));

        assertEquals(2, carrito.getDistinctItemCount());
        assertEquals(7, carrito.getItemCount());
    }

    @Test
    @DisplayName("calcularSubtotal suma el subtotal de cada articulo")
    void calcularSubtotalSumaSubtotales() {
        carrito.agregarArticulo(articulo(UUID.randomUUID(), null, 2, "10.00"));
        carrito.agregarArticulo(articulo(UUID.randomUUID(), null, 1, "5.50"));

        assertEquals(new BigDecimal("25.50"), carrito.calcularSubtotal());
    }

    @Test
    @DisplayName("getTotal resta el montoDescuento del subtotal")
    void getTotalRestaDescuento() {
        carrito.agregarArticulo(articulo(UUID.randomUUID(), null, 2, "10.00"));
        carrito.setMontoDescuento(new BigDecimal("5.00"));

        assertEquals(new BigDecimal("15.00"), carrito.getTotal());
    }

    @Test
    @DisplayName("getTotal nunca es negativo aunque el descuento supere el subtotal")
    void getTotalNoBajaDeCero() {
        carrito.agregarArticulo(articulo(UUID.randomUUID(), null, 1, "10.00"));
        carrito.setMontoDescuento(new BigDecimal("50.00"));

        assertEquals(BigDecimal.ZERO, carrito.getTotal());
    }

    @Test
    @DisplayName("getTotal trata un montoDescuento nulo como cero")
    void getTotalConDescuentoNuloEquivaleACero() {
        carrito.agregarArticulo(articulo(UUID.randomUUID(), null, 1, "10.00"));
        carrito.setMontoDescuento(null);

        assertEquals(new BigDecimal("10.00"), carrito.getTotal());
    }

    @Test
    @DisplayName("isEmpty devuelve false cuando el carrito tiene al menos un articulo")
    void isEmptyFalseConArticulos() {
        carrito.agregarArticulo(articulo(UUID.randomUUID(), null, 1, "1.00"));

        assertFalse(carrito.isEmpty());
    }
}
