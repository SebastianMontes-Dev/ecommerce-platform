package com.ecommerce.modules.catalog.domain;

import com.ecommerce.modules.shared.infrastructure.BaseJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends BaseJpaRepository<Product> {

    Optional<Product> findByIdTiendaAndSlug(UUID idTienda, String slug);

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.images WHERE p.idTienda = :idTienda AND p.slug = :slug")
    Optional<Product> findByIdTiendaAndSlugWithImages(UUID idTienda, String slug);

    Page<Product> findAllByIdTienda(UUID idTienda, Pageable pageable);

    Page<Product> findAllByIdTiendaAndEstado(UUID idTienda, ProductStatus estado, Pageable pageable);

    Page<Product> findAllByIdTiendaAndIdCategoria(UUID idTienda, UUID idCategoria, Pageable pageable);

    long countByIdTienda(UUID idTienda);

    long countByIdTiendaAndEstado(UUID idTienda, ProductStatus estado);
}
