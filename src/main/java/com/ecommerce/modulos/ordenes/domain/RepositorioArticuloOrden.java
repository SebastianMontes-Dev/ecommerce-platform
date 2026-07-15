package com.ecommerce.modulos.ordenes.domain;

import com.ecommerce.modulos.compartido.infrastructure.RepositorioJpaBase;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RepositorioArticuloOrden extends RepositorioJpaBase<ArticuloOrden> {

    List<ArticuloOrden> findAllByIdOrden(UUID idOrden);
}
