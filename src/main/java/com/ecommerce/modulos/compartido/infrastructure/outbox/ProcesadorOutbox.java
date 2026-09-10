package com.ecommerce.modulos.compartido.infrastructure.outbox;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Worker del outbox. En cada tick reclama un lote de filas listas (marcándolas PROCESANDO
 * en una transacción corta con {@code FOR UPDATE SKIP LOCKED}) y luego procesa cada una en
 * su propia transacción, de modo que un fallo aislado no arrastra al resto del lote.
 *
 * <p>Se usan {@link TransactionTemplate} en vez de {@code @Transactional} porque los métodos
 * se llaman entre sí dentro del bean (self-invocation), donde el proxy de Spring no aplica.
 */
@Component
@Slf4j
public class ProcesadorOutbox {

    private final RepositorioEventoOutbox repositorio;
    private final List<ManejadorEventoOutbox> manejadores;
    private final TransactionTemplate txReclamo;
    private final TransactionTemplate txPorFila;

    @Value("${app.outbox.lote:50}")
    private int tamanoLote;

    @Value("${app.outbox.max-intentos:10}")
    private int maxIntentos;

    @Value("${app.outbox.minutos-colgado:2}")
    private long minutosColgado;

    public ProcesadorOutbox(RepositorioEventoOutbox repositorio,
                            List<ManejadorEventoOutbox> manejadores,
                            PlatformTransactionManager txManager) {
        this.repositorio = repositorio;
        this.manejadores = manejadores;
        this.txReclamo = new TransactionTemplate(txManager);
        this.txPorFila = new TransactionTemplate(txManager);
        this.txPorFila.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
    }

    @Scheduled(fixedDelayString = "${app.outbox.intervalo-ms:5000}")
    public void procesar() {
        List<UUID> reclamados = txReclamo.execute(status -> reclamarLote());
        if (reclamados == null || reclamados.isEmpty()) {
            return;
        }
        log.debug("Outbox: procesando {} eventos", reclamados.size());
        for (UUID id : reclamados) {
            try {
                txPorFila.executeWithoutResult(status -> procesarUno(id));
            } catch (Exception e) {
                log.error("Outbox: error no controlado procesando el evento {}", id, e);
            }
        }
    }

    private List<UUID> reclamarLote() {
        LocalDateTime ahora = LocalDateTime.now();
        List<EventoOutbox> lote = repositorio.reclamarLote(
                ahora, ahora.minusMinutes(minutosColgado), tamanoLote);
        lote.forEach(EventoOutbox::marcarEnProceso);
        return lote.stream().map(EventoOutbox::getId).toList();
    }

    void procesarUno(UUID id) {
        EventoOutbox evento = repositorio.findById(id).orElse(null);
        if (evento == null || evento.getEstado() != EstadoEventoOutbox.PROCESANDO) {
            return;
        }
        try {
            manejadorPara(evento.getTipo()).procesar(evento);
            evento.marcarProcesado();
        } catch (Exception e) {
            if (evento.getIntentos() + 1 >= maxIntentos) {
                log.error("Outbox: evento {} ({}) agotó {} reintentos, marcado FALLIDO",
                        id, evento.getTipo(), maxIntentos, e);
                evento.marcarFallido(e.getMessage());
            } else {
                Duration espera = backoff(evento.getIntentos());
                log.warn("Outbox: evento {} ({}) falló (intento {}), reintento en {}",
                        id, evento.getTipo(), evento.getIntentos() + 1, espera);
                evento.reprogramar(e.getMessage(), LocalDateTime.now().plus(espera));
            }
        }
    }

    private ManejadorEventoOutbox manejadorPara(String tipo) {
        return manejadores.stream()
                .filter(m -> m.soporta(tipo))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Sin manejador de outbox para el tipo: " + tipo));
    }

    /** Backoff exponencial acotado: 5s, 10s, 20s, ... máx 5 min. */
    private static Duration backoff(int intentos) {
        long segundos = Math.min(300, 5L << Math.min(intentos, 6));
        return Duration.ofSeconds(segundos);
    }
}
