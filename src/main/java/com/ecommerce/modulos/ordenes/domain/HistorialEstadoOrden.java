package com.ecommerce.modulos.ordenes.domain;

import com.ecommerce.modulos.compartido.domain.EntidadBase;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "order_status_history")
@Getter
@Setter
@NoArgsConstructor
public class HistorialEstadoOrden extends EntidadBase {

    @Column(name = "tenant_id", nullable = false)
    private UUID idTienda;

    @Column(name = "order_id", insertable = false, updatable = false)
    private UUID idOrden;

    @Column(name = "estado_previo")
    private String estadoPrevio;

    @Column(name = "nuevo_estado", nullable = false)
    private String nuevoEstado;

    @Column(name = "changed_by")
    private UUID changedBy;

    @Column(name = "notas", columnDefinition = "TEXT")
    private String notas;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Orden ordenes;
}
