package com.ecommerce.modulos.inquilino.domain;

import com.ecommerce.modulos.compartido.domain.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "subscriptions")
@Getter
@Setter
@NoArgsConstructor
public class Suscripcion extends EntidadInquilino {

    @Column(name = "id_plan", nullable = false)
    private UUID idPlan;

    @Column(name = "estado", nullable = false)
    private String estado = "ACTIVE";

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_plan", insertable = false, updatable = false)
    private PlanSuscripcion plan;

    public boolean isActive() {
        return "ACTIVE".equals(estado) &&
                (fechaFin == null || fechaFin.isAfter(LocalDateTime.now()));
    }

    public void cancel() {
        this.estado = "CANCELLED";
        this.fechaFin = LocalDateTime.now();
    }
}
