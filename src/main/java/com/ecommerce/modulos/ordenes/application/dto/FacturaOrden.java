package com.ecommerce.modulos.ordenes.application.dto;

import com.ecommerce.modulos.compartido.domain.Dinero;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Vista de una orden pensada para renderizar la factura PDF. Es una clase con getters
 * (no un record) porque la consume una plantilla Thymeleaf, que resuelve propiedades por
 * convención {@code getX()}. Deja al módulo consumidor fuera de la entidad {@code Orden}.
 */
@Getter
@Builder
public class FacturaOrden {

    private final String numeroOrden;
    private final String nombreCliente;
    private final String correoCliente;
    private final List<Linea> articulos;
    private final Dinero subtotal;
    private final Dinero montoDescuento;
    private final Dinero montoImpuesto;
    private final Dinero montoEnvio;
    private final Dinero total;

    @Getter
    @Builder
    public static class Linea {
        private final String nombreProducto;
        private final int cantidad;
        private final Dinero precioUnitario;
        private final Dinero subtotal;
    }
}
