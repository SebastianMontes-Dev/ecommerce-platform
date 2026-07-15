package com.ecommerce.modulos.ordenes.application;

import com.ecommerce.modulos.ordenes.domain.*;
import com.ecommerce.modulos.compartido.domain.ExcepcionEntidadNoEncontrada;
import com.ecommerce.modulos.compartido.domain.Dinero;
import com.ecommerce.modulos.compartido.domain.PublicadorEventoDominio;
import com.ecommerce.modulos.compartido.infrastructure.RespuestaPaginada;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CasoUsoOrden {

    private final RepositorioOrden repositorioOrden;
    private final PublicadorEventoDominio eventPublisher;

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

    static RespuestaOrden mapToResponse(Orden ordenes) {
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
