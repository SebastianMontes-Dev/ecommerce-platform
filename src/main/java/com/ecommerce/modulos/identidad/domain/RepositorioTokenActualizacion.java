package com.ecommerce.modulos.identidad.domain;

import com.ecommerce.modulos.compartido.infrastructure.RepositorioJpaBase;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RepositorioTokenActualizacion extends RepositorioJpaBase<TokenActualizacion> {

    Optional<TokenActualizacion> findByToken(String token);

    List<TokenActualizacion> findAllByUserIdAndRevokedFalse(UUID userId);

    // Un token revocado no tiene ningún valor apenas se revoca -se borra sin esperar-; uno
    // expirado se conserva un margen corto por si hace falta auditar un intento de uso tardío
    // antes de purgarlo. Un solo corte de fecha para ambos casos simplifica la query: alcanza
    // con que "antesDe" sea lo bastante viejo para cubrir el margen de los expirados.
    @Modifying
    @Query(value = "DELETE FROM refresh_tokens WHERE revoked = true OR expira_en < :antesDe", nativeQuery = true)
    int eliminarRevocadosOExpiradosAntesDe(@Param("antesDe") LocalDateTime antesDe);
}
