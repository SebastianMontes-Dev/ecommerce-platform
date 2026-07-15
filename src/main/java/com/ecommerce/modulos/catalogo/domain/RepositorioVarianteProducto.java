package com.ecommerce.modulos.catalogo.domain;

import com.ecommerce.modulos.compartido.infrastructure.RepositorioJpaBase;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RepositorioVarianteProducto extends RepositorioJpaBase<VarianteProducto> {

    List<VarianteProducto> findAllByIdProducto(UUID idProducto);
}
