package com.ecommerce.modulos.catalogo.application;

import com.ecommerce.modulos.catalogo.application.dto.*;
import com.ecommerce.modulos.catalogo.domain.*;
import com.ecommerce.modulos.compartido.domain.ExcepcionViolacionReglaNegocio;
import com.ecommerce.modulos.compartido.domain.ExcepcionEntidadNoEncontrada;
import com.ecommerce.modulos.compartido.domain.Dinero;
import com.ecommerce.modulos.compartido.infrastructure.ContextoInquilino;
import com.ecommerce.modulos.compartido.domain.PublicadorEventoDominio;
import com.ecommerce.modulos.catalogo.domain.eventos.EventoProductoCreado;
import com.ecommerce.modulos.inquilino.domain.RepositorioInquilino;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CasoUsoCrearProducto {

    private final RepositorioProducto repositorioProducto;
    private final PublicadorEventoDominio eventPublisher;

    @Transactional
    public RespuestaProducto execute(SolicitudCrearProducto request, UUID idTienda) {
        if (repositorioProducto.countByIdTienda(idTienda) >= 999999) {
            throw new ExcepcionViolacionReglaNegocio("Se ha alcanzado el número máximo de productos para su plan");
        }

        Producto producto = new Producto();
        producto.setIdTienda(idTienda);
        producto.setNombre(request.getNombre());
        producto.setEnlaceCorto(request.getEnlaceCorto());
        producto.setDescripcion(request.getDescripcion());
        producto.setPrecio(Dinero.of(request.getPrecio(), request.getMoneda()));

        if (request.getPrecioComparacion() != null) {
            producto.setPrecioComparacion(Dinero.of(request.getPrecioComparacion(), request.getMoneda()));
        }
        if (request.getPrecioCosto() != null) {
            producto.setPrecioCosto(Dinero.of(request.getPrecioCosto(), request.getMoneda()));
        }

        producto.setSku(request.getSku());
        producto.setCodigoBarras(request.getCodigoBarras());
        producto.setInventario(request.getInventario());
        producto.setRastreoInventarioHabilitado(request.isRastreoInventarioHabilitado());
        producto.setEstado(EstadoProducto.DRAFT);

        if (request.getIdCategoria() != null) {
            producto.setIdCategoria(request.getIdCategoria());
        }

        producto = repositorioProducto.save(producto);

        producto.markAsCreated();
        eventPublisher.publish(producto.getDomainEvents());
        producto.clearDomainEvents();

        return mapToResponse(producto);
    }

    static RespuestaProducto mapToResponse(Producto producto) {
        return RespuestaProducto.builder()
                .id(producto.getId())
                .idTienda(producto.getIdTienda())
                .nombre(producto.getNombre())
                .enlaceCorto(producto.getEnlaceCorto())
                .descripcion(producto.getDescripcion())
                .precio(producto.getPrecio() != null ? producto.getPrecio().getAmount() : null)
                .moneda(producto.getPrecio() != null ? producto.getPrecio().getMoneda() : "USD")
                .precioComparacion(producto.getPrecioComparacion() != null ? producto.getPrecioComparacion().getAmount() : null)
                .precioCosto(producto.getPrecioCosto() != null ? producto.getPrecioCosto().getAmount() : null)
                .sku(producto.getSku())
                .codigoBarras(producto.getCodigoBarras())
                .inventario(producto.getInventario())
                .rastreoInventarioHabilitado(producto.isRastreoInventarioHabilitado())
                .estado(producto.getEstado().name())
                .idCategoria(producto.getIdCategoria())
                .nombreCategoria(producto.getCategoria() != null ? producto.getCategoria().getNombre() : null)
                .variants(producto.getVariants() != null ? producto.getVariants().stream().map(v ->
                        RespuestaVarianteProducto.builder()
                                .id(v.getId())
                                .nombre(v.getNombre())
                                .sku(v.getSku())
                                .precio(v.getAmount())
                                .moneda(v.getMoneda())
                                .inventario(v.getInventario())
                                .attributes(v.getAttributes())
                                .build()
                ).toList() : List.of())
                .images(producto.getImages() != null ? producto.getImages().stream().map(i ->
                        RespuestaImagenProducto.builder()
                                .id(i.getId())
                                .url(i.getUrl())
                                .altText(i.getAltText())
                                .width(i.getWidth())
                                .height(i.getHeight())
                                .sortOrder(i.getSortOrder())
                                .build()
                ).toList() : List.of())
                .creadoEn(producto.getCreadoEn())
                .actualizadoEn(producto.getActualizadoEn())
                .build();
    }
}
