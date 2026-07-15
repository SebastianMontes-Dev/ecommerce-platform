package com.ecommerce.modulos.catalogo.domain;

import com.ecommerce.modulos.compartido.infrastructure.RepositorioJpaBase;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RepositorioImagenProducto extends RepositorioJpaBase<ImagenProducto> {

    List<ImagenProducto> findAllByIdProductoOrderBySortOrderAsc(UUID idProducto);

    void deleteAllByIdProducto(UUID idProducto);
}
