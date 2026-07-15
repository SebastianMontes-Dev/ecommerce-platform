package com.ecommerce.modulos.notificacion.domain;

import com.ecommerce.modulos.compartido.domain.EntidadBase;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
public class Notificacion extends EntidadBase {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "tipo_notificacion", length = 50, nullable = false)
    private String tipoNotificacion;

    @Column(name = "correo_destinatario", nullable = false)
    private String correoDestinatario;

    @Column(name = "subject")
    private String subject;

    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    @Column(name = "estado", length = 20)
    private String estado = "PENDING";

    @Column(name = "enviado_en")
    private LocalDateTime enviadoEn;

    @Column(name = "mensaje_error", columnDefinition = "TEXT")
    private String mensajeError;

    public void markAsSent() {
        this.estado = "SENT";
        this.enviadoEn = LocalDateTime.now();
    }

    public void markAsFailed(String mensajeError) {
        this.estado = "FAILED";
        this.mensajeError = mensajeError;
    }
}
