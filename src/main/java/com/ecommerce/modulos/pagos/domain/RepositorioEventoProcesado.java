package com.ecommerce.modulos.pagos.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface RepositorioEventoProcesado extends JpaRepository<EventoProcesado, String> {

    /**
     * Purga (Fase 11): registros de idempotencia de webhooks de pago más viejos que la
     * retención configurada.
     */
    @Modifying
    @Query(value = "DELETE FROM processed_events WHERE procesado_en < :antesDe", nativeQuery = true)
    int eliminarAntesDe(@Param("antesDe") LocalDateTime antesDe);
}
