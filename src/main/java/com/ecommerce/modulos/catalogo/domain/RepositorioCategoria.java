package com.ecommerce.modulos.catalogo.domain;

import com.ecommerce.modulos.compartido.infrastructure.RepositorioJpaBase;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RepositorioCategoria extends RepositorioJpaBase<Categoria> {

    Optional<Categoria> findByIdTiendaAndEnlaceCorto(UUID idTienda, String enlaceCorto);

    List<Categoria> findAllByIdTiendaAndParentIdIsNull(UUID idTienda);

    List<Categoria> findAllByIdTiendaAndIdPadre(UUID idTienda, UUID idPadre);

    List<Categoria> findAllByIdTiendaAndActiveTrue(UUID idTienda);

    @Query("SELECT c FROM Categoria c LEFT JOIN FETCH c.children WHERE c.idTienda = :idTienda AND c.idPadre IS NULL")
    List<Categoria> findRootCategoriesWithChildren(UUID idTienda);
}
