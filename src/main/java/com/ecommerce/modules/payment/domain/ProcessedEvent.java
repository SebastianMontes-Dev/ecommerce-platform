package com.ecommerce.modules.payment.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "processed_events")
@Getter
@Setter
@NoArgsConstructor
public class ProcessedEvent {

    @Id
    @Column(name = "id_evento", length = 255)
    private String idEvento;

    @Column(name = "tipo_evento", nullable = false)
    private String tipoEvento;

    @Column(name = "procesado_en", nullable = false)
    private LocalDateTime procesadoEn;

    public ProcessedEvent(String idEvento, String tipoEvento) {
        this.idEvento = idEvento;
        this.tipoEvento = tipoEvento;
        this.procesadoEn = LocalDateTime.now();
    }
}
