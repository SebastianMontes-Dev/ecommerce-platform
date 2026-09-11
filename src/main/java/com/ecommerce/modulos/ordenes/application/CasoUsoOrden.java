package com.ecommerce.modulos.ordenes.application;

import com.ecommerce.modulos.carrito.application.ServicioCarrito;
import com.ecommerce.modulos.carrito.domain.Carrito;
import com.ecommerce.modulos.identidad.domain.RepositorioUsuario;
import com.ecommerce.modulos.identidad.domain.Usuario;
import com.ecommerce.modulos.ordenes.application.dto.SolicitudCheckout;
import com.ecommerce.modulos.ordenes.domain.*;
import com.ecommerce.modulos.compartido.domain.ExcepcionEntidadNoEncontrada;
import com.ecommerce.modulos.compartido.domain.ExcepcionOperacionInvalida;
import com.ecommerce.modulos.compartido.domain.Dinero;
import com.ecommerce.modulos.compartido.domain.PublicadorEventoDominio;
import com.ecommerce.modulos.compartido.infrastructure.RespuestaPaginada;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CasoUsoOrden {

    private final RepositorioOrden repositorioOrden;
    private final ServicioCarrito servicioCarrito;
    private final RepositorioUsuario repositorioUsuario;
    private final PublicadorEventoDominio eventPublisher;
    private final RepositorioCupon repositorioCupon;
    private final com.ecommerce.modulos.compartido.infrastructure.websocket.ServicioNotificacionTiempoReal servicioNotificacionTiempoReal;
    private final com.ecommerce.modulos.compartido.infrastructure.observabilidad.MetricasNegocio metricasNegocio;

    @Transactional
    public RespuestaOrden createOrderFromCart(UUID idCliente, UUID idTienda, SolicitudCheckout request) {
        Carrito carrito = servicioCarrito.getOrCreateCart(idCliente, idTienda);
        
        if (carrito.isEmpty()) {
            throw new ExcepcionOperacionInvalida("Cannot checkout with an empty cart");
        }

        Usuario usuario = repositorioUsuario.findById(idCliente)
                .orElseThrow(() -> new ExcepcionEntidadNoEncontrada("Usuario", idCliente));

        Orden orden = new Orden();
        orden.setIdTienda(idTienda);
        orden.setIdCliente(idCliente);
        orden.setCorreoCliente(usuario.getCorreo());
        orden.setNombreCliente(usuario.getNombreCompleto());
        orden.setNumeroOrden(UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        
        orden.setDireccionEnvio(request.getDireccionEnvio());
        orden.setDireccionFacturacion(request.getDireccionFacturacion());
        orden.setNotas(request.getNotas());

        BigDecimal subtotal = BigDecimal.ZERO;
        String currency = "USD";

        for (var item : carrito.getArticulos()) {
            ArticuloOrden articuloOrden = new ArticuloOrden();
            articuloOrden.setIdTienda(idTienda);
            articuloOrden.setIdProducto(item.getIdProducto());
            articuloOrden.setNombreProducto(item.getNombreProducto());
            articuloOrden.setVariantId(item.getVariantId());
            articuloOrden.setVariantName(item.getVariantName());
            articuloOrden.setCantidad(item.getCantidad());
            articuloOrden.setPrecioUnitario(Dinero.of(item.getPrecioUnitario(), item.getMoneda()));
            articuloOrden.setSubtotal(Dinero.of(item.getSubtotal(), item.getMoneda()));
            articuloOrden.setOrdenes(orden);
            // No need to manually set idOrden, Hibernate will handle it because ordenes owns the relationship
            orden.getArticulos().add(articuloOrden);
            
            subtotal = subtotal.add(item.getSubtotal());
            currency = item.getMoneda();
        }

        orden.setSubtotal(Dinero.of(subtotal, currency));

        BigDecimal descuento = BigDecimal.ZERO;
        if (carrito.getCodigoCupon() != null) {
            Cupon cupon = repositorioCupon.findByIdTiendaAndCodigo(idTienda, carrito.getCodigoCupon())
                    .orElse(null);
            
            if (cupon != null && cupon.esValido()) {
                descuento = carrito.getMontoDescuento() != null ? carrito.getMontoDescuento() : BigDecimal.ZERO;
                orden.setCodigoCupon(cupon.getCodigo());
                orden.setMontoDescuento(Dinero.of(descuento, currency));
                
                cupon.registrarUso();
                repositorioCupon.save(cupon);
            }
        }
        
        // Simple logic for tax and shipping
        BigDecimal subtotalConDescuento = subtotal.subtract(descuento).max(BigDecimal.ZERO);
        BigDecimal tax = subtotalConDescuento.multiply(new BigDecimal("0.10"));
        BigDecimal shipping = new BigDecimal("10.00");
        
        orden.setMontoImpuesto(Dinero.of(tax, currency));
        orden.setMontoEnvio(Dinero.of(shipping, currency));
        orden.setTotal(Dinero.of(subtotalConDescuento.add(tax).add(shipping), currency));

        orden.markAsCreated();
        orden = repositorioOrden.save(orden);
        // Publica EventoOrdenCreada: ManejadorEventosOrden reserva el inventario aquí,
        // en esta misma transacción. Si no hay stock, lanza y todo hace rollback.
        eventPublisher.publish(orden.getDomainEvents());
        orden.clearDomainEvents();

        RespuestaOrden respuesta = mapToResponse(orden);

        // Efectos que NO deben ocurrir si la transacción hace rollback: vaciar el carrito
        // en Redis dejaría al cliente sin carrito para una orden inexistente, y la
        // notificación en vivo anunciaría una orden fantasma.
        ejecutarTrasCommit(() -> {
            servicioCarrito.clearCart(idCliente, idTienda);
            servicioNotificacionTiempoReal.notificarNuevaOrden(idTienda, respuesta);
            metricasNegocio.ordenCreada();
        });

        return respuesta;
    }

    /**
     * Ejecuta {@code accion} después de que la transacción actual haga commit. Si no hay
     * transacción activa (p. ej. en tests unitarios), la ejecuta de inmediato.
     */
    private void ejecutarTrasCommit(Runnable accion) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    accion.run();
                }
            });
        } else {
            accion.run();
        }
    }

    @Transactional(readOnly = true)
    public RespuestaOrden getOrder(UUID idOrden, UUID idTienda, UUID idCliente) {
        Orden ordenes = repositorioOrden.findById(idOrden)
                .orElseThrow(() -> new ExcepcionEntidadNoEncontrada("Orden", idOrden));
        if (!ordenes.getIdTienda().equals(idTienda)) {
            throw new ExcepcionEntidadNoEncontrada("Orden", idOrden);
        }
        if (!ordenes.getIdCliente().equals(idCliente)) {
            throw new ExcepcionEntidadNoEncontrada("Orden", idOrden);
        }
        return mapToResponse(ordenes);
    }

    @Transactional(readOnly = true)
    public RespuestaPaginada<RespuestaOrden> listOrdersByTenant(UUID idTienda, Pageable pageable) {
        Page<Orden> page = repositorioOrden.findAllByIdTienda(idTienda, pageable);
        return RespuestaPaginada.from(page.map(CasoUsoOrden::mapToResponse));
    }

    @Transactional(readOnly = true)
    public RespuestaPaginada<RespuestaOrden> listOrdersByCustomer(UUID idCliente, Pageable pageable) {
        Page<Orden> page = repositorioOrden.findAllByIdCliente(idCliente, pageable);
        return RespuestaPaginada.from(page.map(CasoUsoOrden::mapToResponse));
    }

    @Transactional
    public RespuestaOrden cancelOrder(UUID idOrden, UUID idTienda, UUID idCliente, String reason) {
        Orden ordenes = repositorioOrden.findById(idOrden)
                .orElseThrow(() -> new ExcepcionEntidadNoEncontrada("Orden", idOrden));
        if (!ordenes.getIdTienda().equals(idTienda)) {
            throw new ExcepcionEntidadNoEncontrada("Orden", idOrden);
        }
        if (!ordenes.getIdCliente().equals(idCliente)) {
            throw new ExcepcionEntidadNoEncontrada("Orden", idOrden);
        }
        ordenes.cancel(reason);
        ordenes = repositorioOrden.save(ordenes);
        eventPublisher.publish(ordenes.getDomainEvents());
        ordenes.clearDomainEvents();
        return mapToResponse(ordenes);
    }

    public static RespuestaOrden mapToResponse(Orden ordenes) {
        return RespuestaOrden.builder()
                .id(ordenes.getId())
                .numeroOrden(ordenes.getNumeroOrden())
                .idCliente(ordenes.getIdCliente())
                .correoCliente(ordenes.getCorreoCliente())
                .nombreCliente(ordenes.getNombreCliente())
                .subtotal(ordenes.getSubtotal() != null ? ordenes.getSubtotal().getMonto() : null)
                .montoImpuesto(ordenes.getMontoImpuesto() != null ? ordenes.getMontoImpuesto().getMonto() : null)
                .montoEnvio(ordenes.getMontoEnvio() != null ? ordenes.getMontoEnvio().getMonto() : null)
                .total(ordenes.getTotal() != null ? ordenes.getTotal().getMonto() : null)
                .moneda(ordenes.getTotal() != null ? ordenes.getTotal().getMoneda() : "USD")
                .estado(ordenes.getEstado().name())
                .notas(ordenes.getNotas())
                .creadoEn(ordenes.getCreadoEn())
                .actualizadoEn(ordenes.getActualizadoEn())
                .build();
    }
}
