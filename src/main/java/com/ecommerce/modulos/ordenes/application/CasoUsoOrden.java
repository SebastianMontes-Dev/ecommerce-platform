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

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CasoUsoOrden {

    private final RepositorioOrden repositorioOrden;
    private final ServicioCarrito servicioCarrito;
    private final RepositorioUsuario repositorioUsuario;
    private final PublicadorEventoDominio eventPublisher;

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
        orden.setNotes(request.getNotes());

        BigDecimal subtotal = BigDecimal.ZERO;
        String currency = "USD";

        for (var item : carrito.getArticulos()) {
            ArticuloOrden articuloOrden = new ArticuloOrden();
            articuloOrden.setIdTienda(idTienda);
            articuloOrden.setIdProducto(item.getIdProducto());
            articuloOrden.setNombreProducto(item.getNombreProducto());
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
        // Simple logic for tax and shipping
        BigDecimal tax = subtotal.multiply(new BigDecimal("0.10"));
        BigDecimal shipping = new BigDecimal("10.00");
        orden.setMontoImpuesto(Dinero.of(tax, currency));
        orden.setMontoEnvio(Dinero.of(shipping, currency));
        orden.setTotal(Dinero.of(subtotal.add(tax).add(shipping), currency));

        orden.markAsCreated();
        orden = repositorioOrden.save(orden);
        eventPublisher.publish(orden.getDomainEvents());
        orden.clearDomainEvents();

        servicioCarrito.clearCart(idCliente);

        return mapToResponse(orden);
    }

    @Transactional(readOnly = true)
    public RespuestaOrden getOrder(UUID idOrden, UUID idTienda) {
        Orden ordenes = repositorioOrden.findById(idOrden)
                .orElseThrow(() -> new ExcepcionEntidadNoEncontrada("Orden", idOrden));
        if (!ordenes.getIdTienda().equals(idTienda)) {
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
    public RespuestaOrden cancelOrder(UUID idOrden, UUID idTienda, String reason) {
        Orden ordenes = repositorioOrden.findById(idOrden)
                .orElseThrow(() -> new ExcepcionEntidadNoEncontrada("Orden", idOrden));
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
                .subtotal(ordenes.getSubtotal() != null ? ordenes.getSubtotal().getAmount() : null)
                .montoImpuesto(ordenes.getMontoImpuesto() != null ? ordenes.getMontoImpuesto().getAmount() : null)
                .montoEnvio(ordenes.getMontoEnvio() != null ? ordenes.getMontoEnvio().getAmount() : null)
                .total(ordenes.getTotal() != null ? ordenes.getTotal().getAmount() : null)
                .moneda(ordenes.getTotal() != null ? ordenes.getTotal().getMoneda() : "USD")
                .estado(ordenes.getEstado().name())
                .notes(ordenes.getNotes())
                .creadoEn(ordenes.getCreadoEn())
                .actualizadoEn(ordenes.getActualizadoEn())
                .build();
    }
}
