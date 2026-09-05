package com.ecommerce.modulos.catalogo.domain;

import com.ecommerce.modulos.compartido.domain.Dinero;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class VarianteProductoTest {

    private VarianteProducto variante;

    @BeforeEach
    void setUp() {
        variante = new VarianteProducto();
        variante.setNombre("Rojo - M");
    }

    @Test
    void debeUsarSuPropioMontoYMonedaCuandoLosTiene() {
        variante.setMonto(new BigDecimal("99.00"));
        variante.setMoneda("USD");

        Dinero precio = variante.getPrecio();

        assertEquals(0, new BigDecimal("99.00").compareTo(precio.getMonto()));
        assertEquals("USD", precio.getMoneda());
    }

    @Test
    void debeHeredarElPrecioDelProductoBaseCuandoNoTieneMontoPropio() {
        variante.setMonto(null);
        Producto producto = new Producto();
        producto.setPrecio(Dinero.of(new BigDecimal("150.00"), "USD"));
        variante.setProducto(producto);

        Dinero precio = variante.getPrecio();

        assertEquals(0, new BigDecimal("150.00").compareTo(precio.getMonto()));
    }

    @Test
    void debeRetornarNuloCuandoNoTieneMontoPropioNiProductoAsociado() {
        variante.setMonto(null);
        variante.setProducto(null);

        assertNull(variante.getPrecio());
    }

    @Test
    void debeReducirInventarioCuandoHayStockSuficiente() {
        variante.setInventario(10);

        variante.decreaseInventory(4);

        assertEquals(6, variante.getInventario());
    }

    @Test
    void debeLanzarExcepcionAlReducirInventarioInsuficiente() {
        variante.setInventario(2);

        assertThrows(IllegalStateException.class, () -> variante.decreaseInventory(5));
    }

    @Test
    void debePermitirReducirTodoElInventarioHastaCero() {
        variante.setInventario(5);

        variante.decreaseInventory(5);

        assertEquals(0, variante.getInventario());
    }
}
