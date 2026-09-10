package com.ecommerce.modulos.compartido.infrastructure.outbox;

import com.ecommerce.modulos.compartido.infrastructure.RepositorioJpaBase;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface RepositorioEventoOutbox extends RepositorioJpaBase<EventoOutbox> {

    /**
     * Filas listas para procesar: PENDIENTE, o PROCESANDO que quedaron colgadas (worker
     * caído). {@code FOR UPDATE SKIP LOCKED} permite que varias instancias de la app
     * tomen lotes disjuntos sin bloquearse entre sí.
     */
    @Query(value = """
            SELECT * FROM outbox_eventos
            WHERE proximo_intento_en <= :ahora
              AND (estado = 'PENDIENTE'
                   OR (estado = 'PROCESANDO' AND reclamado_en < :limiteColgado))
            ORDER BY created_at
            LIMIT :limite
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<EventoOutbox> reclamarLote(@Param("ahora") LocalDateTime ahora,
                                    @Param("limiteColgado") LocalDateTime limiteColgado,
                                    @Param("limite") int limite);

    long countByEstado(EstadoEventoOutbox estado);

    List<EventoOutbox> findByAgregadoIdAndTipo(UUID agregadoId, String tipo);
}
