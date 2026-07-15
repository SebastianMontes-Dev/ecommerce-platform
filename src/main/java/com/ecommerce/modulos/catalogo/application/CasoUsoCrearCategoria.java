package com.ecommerce.modulos.catalogo.application;

import com.ecommerce.modulos.catalogo.application.dto.*;
import com.ecommerce.modulos.catalogo.domain.*;
import com.ecommerce.modulos.compartido.domain.ExcepcionOperacionInvalida;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CasoUsoCrearCategoria {

    private final RepositorioCategoria repositorioCategoria;

    @Transactional
    public RespuestaCategoria execute(SolicitudCrearCategoria request, UUID idTienda) {
        Categoria categoria = new Categoria();
        categoria.setIdTienda(idTienda);
        categoria.setNombre(request.getNombre());
        categoria.setEnlaceCorto(request.getEnlaceCorto());
        categoria.setDescripcion(request.getDescripcion());
        categoria.setUrlImagen(request.getUrlImagen());

        if (request.getIdPadre() != null) {
            categoria.setIdPadre(request.getIdPadre());
        }

        categoria = repositorioCategoria.save(categoria);
        return mapToResponse(categoria);
    }

    @Transactional(readOnly = true)
    public List<RespuestaCategoria> getCategories(UUID idTienda) {
        return repositorioCategoria.findRootCategoriesWithChildren(idTienda).stream()
                .map(CasoUsoCrearCategoria::mapToResponse)
                .toList();
    }

    static RespuestaCategoria mapToResponse(Categoria categoria) {
        return RespuestaCategoria.builder()
                .id(categoria.getId())
                .nombre(categoria.getNombre())
                .enlaceCorto(categoria.getEnlaceCorto())
                .descripcion(categoria.getDescripcion())
                .urlImagen(categoria.getUrlImagen())
                .idPadre(categoria.getIdPadre())
                .children(categoria.getChildren() != null ? categoria.getChildren().stream()
                        .map(CasoUsoCrearCategoria::mapToChildResponse).toList() : List.of())
                .build();
    }

    static RespuestaCategoria mapToChildResponse(Categoria categoria) {
        return RespuestaCategoria.builder()
                .id(categoria.getId())
                .nombre(categoria.getNombre())
                .enlaceCorto(categoria.getEnlaceCorto())
                .descripcion(categoria.getDescripcion())
                .urlImagen(categoria.getUrlImagen())
                .idPadre(categoria.getIdPadre())
                .children(List.of())
                .build();
    }
}
