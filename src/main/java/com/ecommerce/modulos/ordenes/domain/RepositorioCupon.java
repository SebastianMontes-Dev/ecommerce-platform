package com.ecommerce.modulos.ordenes.domain;

import com.ecommerce.modulos.compartido.infrastructure.RepositorioJpaBase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RepositorioCupon extends RepositorioJpaBase<Cupon> {
    Optional<Cupon> findByIdTiendaAndCodigo(UUID idTienda, String codigo);
    Page<Cupon> findAllByIdTienda(UUID idTienda, Pageable pageable);
}
