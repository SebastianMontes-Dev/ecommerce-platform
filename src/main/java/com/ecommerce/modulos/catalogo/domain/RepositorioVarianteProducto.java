package com.ecommerce.modulos.catalogo.domain;

import com.ecommerce.modulos.compartido.infrastructure.RepositorioJpaBase;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RepositorioVarianteProducto extends RepositorioJpaBase<VarianteProducto> {

    List<VarianteProducto> findAllByIdProducto(UUID idProducto);

    /**
     * Bloquea la fila de la variante ({@code SELECT ... FOR UPDATE}) para descontar o
     * reponer inventario sin condiciones de carrera / sobreventa.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM VarianteProducto v WHERE v.id = :id")
    Optional<VarianteProducto> findByIdForUpdate(UUID id);
}
