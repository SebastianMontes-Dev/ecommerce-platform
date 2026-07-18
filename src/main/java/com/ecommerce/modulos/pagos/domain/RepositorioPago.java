package com.ecommerce.modulos.pagos.domain;

import com.ecommerce.modulos.compartido.infrastructure.RepositorioJpaBase;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RepositorioPago extends RepositorioJpaBase<Pago> {

    Optional<Pago> findByIdOrden(UUID idOrden);

    Optional<Pago> findByIdExterno(String idExterno);

    @org.springframework.data.jpa.repository.Query(value = "SELECT * FROM pagos WHERE id_externo = :idExterno", nativeQuery = true)
    Optional<Pago> findByIdExternoSinFiltro(@org.springframework.data.repository.query.Param("idExterno") String idExterno);
}
