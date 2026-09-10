package com.ecommerce.modulos.ordenes.application;

import com.ecommerce.modulos.compartido.domain.Dinero;
import com.ecommerce.modulos.compartido.domain.ExcepcionEntidadNoEncontrada;
import com.ecommerce.modulos.compartido.domain.ExcepcionOperacionInvalida;
import com.ecommerce.modulos.ordenes.application.dto.FacturaOrden;
import com.ecommerce.modulos.ordenes.domain.EstadoOrden;
import com.ecommerce.modulos.ordenes.domain.Orden;
import com.ecommerce.modulos.ordenes.domain.RepositorioOrden;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * API de <b>lectura</b> del módulo <b>ordenes</b> para otros módulos. Devuelve DTOs, nunca
 * la entidad {@code Orden} ni el repositorio: así <b>notificacion</b> puede armar sus correos
 * y su factura sin acoplarse a la persistencia de ordenes.
 */
@Service
@RequiredArgsConstructor
public class ServicioConsultaOrden {

    private final RepositorioOrden repositorioOrden;

    /** Datos mínimos para los correos transaccionales (número, destinatario, total). */
    @Transactional(readOnly = true)
    public ResumenOrden obtenerResumen(UUID idOrden) {
        Orden orden = repositorioOrden.findById(idOrden)
                .orElseThrow(() -> new ExcepcionEntidadNoEncontrada("Orden", idOrden));
        return new ResumenOrden(
                orden.getNumeroOrden(),
                orden.getCorreoCliente(),
                orden.getNombreCliente() != null ? orden.getNombreCliente() : "Cliente",
                formatear(orden.getTotal()));
    }

    /** Vista completa (con líneas) para renderizar la factura PDF. */
    @Transactional(readOnly = true)
    public FacturaOrden obtenerFactura(UUID idOrden) {
        Orden orden = repositorioOrden.findByIdConArticulos(idOrden)
                .orElseThrow(() -> new ExcepcionEntidadNoEncontrada("Orden", idOrden));
        return FacturaOrden.builder()
                .numeroOrden(orden.getNumeroOrden())
                .nombreCliente(orden.getNombreCliente() != null ? orden.getNombreCliente() : "Cliente")
                .correoCliente(orden.getCorreoCliente())
                .subtotal(orden.getSubtotal())
                .montoDescuento(orden.getMontoDescuento())
                .montoImpuesto(orden.getMontoImpuesto())
                .montoEnvio(orden.getMontoEnvio())
                .total(orden.getTotal())
                .articulos(orden.getArticulos().stream()
                        .map(a -> FacturaOrden.Linea.builder()
                                .nombreProducto(a.getNombreProducto())
                                .cantidad(a.getCantidad())
                                .precioUnitario(a.getPrecioUnitario())
                                .subtotal(a.getSubtotal())
                                .build())
                        .toList())
                .build();
    }

    /**
     * Verifica que {@code idCliente} puede reseñar {@code idProducto}: la orden es suya,
     * está {@code DELIVERED} y contiene ese producto ("compra verificada"). Lanza
     * {@link ExcepcionOperacionInvalida} con el motivo exacto si no. Evita que el módulo
     * de reseñas tenga que conocer la entidad {@code Orden}.
     */
    @Transactional(readOnly = true)
    public void verificarElegibilidadResena(UUID idOrden, UUID idCliente, UUID idProducto) {
        Orden orden = repositorioOrden.findByIdConArticulos(idOrden)
                .orElseThrow(() -> new ExcepcionEntidadNoEncontrada("Orden", idOrden));

        if (!orden.getIdCliente().equals(idCliente)) {
            throw new ExcepcionOperacionInvalida("La orden no pertenece al usuario autenticado.");
        }
        if (orden.getEstado() != EstadoOrden.DELIVERED) {
            throw new ExcepcionOperacionInvalida("Solo se pueden reseñar productos de órdenes entregadas.");
        }
        boolean contieneProducto = orden.getArticulos().stream()
                .anyMatch(articulo -> articulo.getIdProducto().equals(idProducto));
        if (!contieneProducto) {
            throw new ExcepcionOperacionInvalida("La orden no contiene el producto especificado.");
        }
    }

    private static String formatear(Dinero dinero) {
        return dinero != null ? dinero.getMonto() + " " + dinero.getMoneda() : "";
    }

    public record ResumenOrden(String numeroOrden, String correoCliente, String nombreCliente, String totalFormateado) {}
}
