package com.ecommerce.modulos.catalogo.domain;

import com.ecommerce.modulos.compartido.infrastructure.RepositorioJpaBase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RepositorioProducto extends RepositorioJpaBase<Producto> {

    Optional<Producto> findByIdTiendaAndEnlaceCorto(UUID idTienda, String enlaceCorto);

    @Query("SELECT p FROM Producto p LEFT JOIN FETCH p.images WHERE p.idTienda = :idTienda AND p.enlaceCorto = :enlaceCorto")
    Optional<Producto> findByIdTiendaAndEnlaceCortoWithImages(UUID idTienda, String enlaceCorto);

    @Query("SELECT p FROM Producto p LEFT JOIN FETCH p.variants WHERE p.id = :id")
    Optional<Producto> findByIdWithVariants(UUID id);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Producto p WHERE p.id = :id")
    Optional<Producto> findByIdForUpdate(UUID id);

    Page<Producto> findAllByIdTienda(UUID idTienda, Pageable pageable);

    Page<Producto> findAllByIdTiendaAndEstado(UUID idTienda, EstadoProducto estado, Pageable pageable);

    Page<Producto> findAllByIdTiendaAndIdCategoria(UUID idTienda, UUID idCategoria, Pageable pageable);

    long countByIdTienda(UUID idTienda);

    long countByIdTiendaAndEstado(UUID idTienda, EstadoProducto estado);
}
