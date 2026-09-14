package com.ecommerce.modulos.compartido.infrastructure.purga;

import com.ecommerce.modulos.compartido.infrastructure.outbox.RepositorioEventoOutbox;
import com.ecommerce.modulos.identidad.domain.RepositorioTokenActualizacion;
import com.ecommerce.modulos.pagos.domain.RepositorioEventoProcesado;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Housekeeping de las tablas de crecimiento no acotado del proyecto (ninguna tenía
 * mecanismo de retención antes de la Fase 11): outbox_eventos, refresh_tokens,
 * processed_events. Corre una vez al día por defecto; cada retención es configurable
 * por entorno.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ServicioPurgaDatos {

    private final RepositorioEventoOutbox repositorioEventoOutbox;
    private final RepositorioTokenActualizacion repositorioTokenActualizacion;
    private final RepositorioEventoProcesado repositorioEventoProcesado;

    @Value("${app.purga.outbox-dias:30}")
    private int diasRetencionOutbox;

    @Value("${app.purga.refresh-tokens-dias:7}")
    private int diasRetencionRefreshTokens;

    @Value("${app.purga.eventos-procesados-dias:90}")
    private int diasRetencionEventosProcesados;

    @Scheduled(cron = "${app.purga.cron:0 0 3 * * *}")
    @Transactional
    public void purgar() {
        LocalDateTime ahora = LocalDateTime.now();
        int outbox = repositorioEventoOutbox.eliminarProcesadosAntesDe(ahora.minusDays(diasRetencionOutbox));
        int tokens = repositorioTokenActualizacion.eliminarRevocadosOExpiradosAntesDe(ahora.minusDays(diasRetencionRefreshTokens));
        int eventos = repositorioEventoProcesado.eliminarAntesDe(ahora.minusDays(diasRetencionEventosProcesados));
        log.info("Purga de datos: {} eventos de outbox, {} refresh tokens, {} eventos procesados eliminados",
                outbox, tokens, eventos);
    }
}
