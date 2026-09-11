package com.ecommerce.modulos.ordenes.domain;

import com.ecommerce.modulos.compartido.infrastructure.RepositorioJpaBase;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RepositorioCupon extends RepositorioJpaBase<Cupon> {
    Optional<Cupon> findByIdTiendaAndCodigo(UUID idTienda, String codigo);
    Page<Cupon> findAllByIdTienda(UUID idTienda, Pageable pageable);

    // Mismo patrón que RepositorioProducto.findByIdForUpdate: sin este lock, dos checkouts
    // concurrentes con el mismo cupón de limiteUsos=1 leen esValido()==true antes de que
    // cualquiera haga commit, y los dos registran el uso -sobreventa del cupón, igual que
    // pasaba con el stock antes de la Fase 1-.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Cupon c WHERE c.idTienda = :idTienda AND c.codigo = :codigo")
    Optional<Cupon> findByIdTiendaAndCodigoForUpdate(UUID idTienda, String codigo);
}
