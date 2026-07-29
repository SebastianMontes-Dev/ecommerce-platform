package com.ecommerce.modulos.logistica.domain;

import com.ecommerce.modulos.compartido.domain.EntidadInquilino;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "envios")
@Getter
@Setter
@NoArgsConstructor
public class Envio extends EntidadInquilino {

    @Column(name = "id_orden", nullable = false, unique = true)
    private UUID idOrden;

    @Column(name = "numero_guia")
    private String numeroGuia; // Tracking number

    @Column(name = "proveedor")
    private String proveedor; // Ej: DHL, FedEx

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoEnvio estado = EstadoEnvio.PREPARANDO;

    @OneToMany(mappedBy = "envio", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("creadoEn DESC")
    private List<EventoTracking> historial = new ArrayList<>();

    public void actualizarEstado(EstadoEnvio nuevoEstado, String ubicacion, String descripcion) {
        this.estado = nuevoEstado;
        EventoTracking evento = new EventoTracking();
        evento.setEnvio(this);
        evento.setIdTienda(this.getIdTienda());
        evento.setEstado(nuevoEstado);
        evento.setUbicacion(ubicacion);
        evento.setDescripcion(descripcion);
        historial.add(evento);
    }
}
