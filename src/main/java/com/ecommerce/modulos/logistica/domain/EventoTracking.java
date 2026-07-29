package com.ecommerce.modulos.logistica.domain;

import com.ecommerce.modulos.compartido.domain.EntidadInquilino;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "evento_tracking")
@Getter
@Setter
@NoArgsConstructor
public class EventoTracking extends EntidadInquilino {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_envio", nullable = false)
    @JsonIgnore
    private Envio envio;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoEnvio estado;

    @Column(name = "ubicacion")
    private String ubicacion;

    @Column(name = "descripcion")
    private String descripcion;
}
