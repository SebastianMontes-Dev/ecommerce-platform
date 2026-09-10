package com.ecommerce.modulos.compartido.infrastructure.outbox;

import com.ecommerce.modulos.compartido.domain.EntidadBase;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Fila de la tabla outbox. Se escribe en la misma transacción que el cambio de negocio;
 * un worker ({@link ProcesadorOutbox}) la entrega al sistema externo con reintentos.
 */
@Entity
@Table(name = "outbox_eventos")
@Getter
@NoArgsConstructor
public class EventoOutbox extends EntidadBase {

    @Column(name = "tipo", nullable = false, length = 100)
    private String tipo;

    @Column(name = "agregado_id", nullable = false)
    private UUID agregadoId;

    @Column(name = "tenant_id", nullable = false)
    private UUID idTienda;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoEventoOutbox estado = EstadoEventoOutbox.PENDIENTE;

    @Column(name = "intentos", nullable = false)
    private int intentos = 0;

    @Column(name = "ultimo_error", columnDefinition = "TEXT")
    private String ultimoError;

    @Column(name = "reclamado_en")
    private LocalDateTime reclamadoEn;

    @Column(name = "procesado_en")
    private LocalDateTime procesadoEn;

    @Column(name = "proximo_intento_en", nullable = false)
    private LocalDateTime proximoIntentoEn = LocalDateTime.now();

    public EventoOutbox(String tipo, UUID agregadoId, UUID idTienda, String payload) {
        this.tipo = tipo;
        this.agregadoId = agregadoId;
        this.idTienda = idTienda;
        this.payload = payload;
    }

    public void marcarEnProceso() {
        this.estado = EstadoEventoOutbox.PROCESANDO;
        this.reclamadoEn = LocalDateTime.now();
    }

    public void marcarProcesado() {
        this.estado = EstadoEventoOutbox.PROCESADO;
        this.procesadoEn = LocalDateTime.now();
        this.ultimoError = null;
    }

    /** Vuelve a PENDIENTE para reintentar más tarde. */
    public void reprogramar(String error, LocalDateTime proximoIntento) {
        this.intentos++;
        this.ultimoError = recortar(error);
        this.proximoIntentoEn = proximoIntento;
        this.estado = EstadoEventoOutbox.PENDIENTE;
        this.reclamadoEn = null;
    }

    public void marcarFallido(String error) {
        this.intentos++;
        this.ultimoError = recortar(error);
        this.estado = EstadoEventoOutbox.FALLIDO;
    }

    private static String recortar(String s) {
        if (s == null) return null;
        return s.length() > 4000 ? s.substring(0, 4000) : s;
    }
}
