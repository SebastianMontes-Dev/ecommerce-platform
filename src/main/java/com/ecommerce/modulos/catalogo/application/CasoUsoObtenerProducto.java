package com.ecommerce.modulos.catalogo.application;

import com.ecommerce.modulos.catalogo.application.dto.RespuestaProducto;
import com.ecommerce.modulos.catalogo.domain.*;
import com.ecommerce.modulos.compartido.domain.ExcepcionEntidadNoEncontrada;
import com.ecommerce.modulos.compartido.infrastructure.RespuestaPaginada;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CasoUsoObtenerProducto {

    private final RepositorioProducto repositorioProducto;

    @Transactional(readOnly = true)
    public RespuestaProducto bySlug(String enlaceCorto, UUID idTienda) {
        Producto producto = repositorioProducto.findByIdTiendaAndEnlaceCortoWithImages(idTienda, enlaceCorto)
                .orElseThrow(() -> new ExcepcionEntidadNoEncontrada("Producto", enlaceCorto));
        return CasoUsoCrearProducto.mapToResponse(producto);
    }

    @Transactional(readOnly = true)
    public RespuestaProducto byId(UUID id, UUID idTienda) {
        Producto producto = repositorioProducto.findById(id)
                .orElseThrow(() -> new ExcepcionEntidadNoEncontrada("Producto", id));

        if (!producto.getIdTienda().equals(idTienda)) {
            throw new ExcepcionEntidadNoEncontrada("Producto", id);
        }

        return CasoUsoCrearProducto.mapToResponse(producto);
    }

    @Transactional(readOnly = true)
    public RespuestaPaginada<RespuestaProducto> listProducts(UUID idTienda, Pageable pageable) {
        Page<Producto> page = repositorioProducto.findAllByIdTienda(idTienda, pageable);
        return RespuestaPaginada.from(page.map(CasoUsoCrearProducto::mapToResponse));
    }
}
