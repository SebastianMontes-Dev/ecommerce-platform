package com.ecommerce.modulos.ordenes.domain;

import com.ecommerce.modulos.compartido.infrastructure.RepositorioJpaBase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RepositorioOrden extends RepositorioJpaBase<Orden> {

    Optional<Orden> findByNumeroOrden(String numeroOrden);

    @Query("SELECT o FROM Orden o LEFT JOIN FETCH o.articulos WHERE o.id = :id")
    Optional<Orden> findByIdConArticulos(UUID id);

    Page<Orden> findAllByIdTienda(UUID idTienda, Pageable pageable);

    java.util.List<Orden> findAllByIdTienda(UUID idTienda);

    Page<Orden> findAllByIdCliente(UUID idCliente, Pageable pageable);
}
