package com.ecommerce.modulos.ordenes.domain;

import com.ecommerce.modulos.compartido.domain.*;
import com.ecommerce.modulos.ordenes.domain.eventos.EventoEstadoOrdenCambiado;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "ordenes")
@Getter
@Setter
@NoArgsConstructor
public class Orden extends RaizAgregadaInquilino {

    @Column(name = "numero_orden", unique = true, nullable = false, length = 100)
    private String numeroOrden;

    @Column(name = "id_cliente", nullable = false)
    private UUID idCliente;

    @Column(name = "correo_cliente", nullable = false)
    private String correoCliente;

    @Column(name = "nombre_cliente")
    private String nombreCliente;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "street", column = @Column(name = "shipping_street")),
            @AttributeOverride(name = "city", column = @Column(name = "shipping_city")),
            @AttributeOverride(name = "state", column = @Column(name = "shipping_state")),
            @AttributeOverride(name = "codigoPostal", column = @Column(name = "shipping_zip_code")),
            @AttributeOverride(name = "country", column = @Column(name = "shipping_country")),
            @AttributeOverride(name = "additionalInfo", column = @Column(name = "shipping_additional_info"))
    })
    private Direccion direccionEnvio;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "street", column = @Column(name = "billing_street")),
            @AttributeOverride(name = "city", column = @Column(name = "billing_city")),
            @AttributeOverride(name = "state", column = @Column(name = "billing_state")),
            @AttributeOverride(name = "codigoPostal", column = @Column(name = "billing_zip_code")),
            @AttributeOverride(name = "country", column = @Column(name = "billing_country")),
            @AttributeOverride(name = "additionalInfo", column = @Column(name = "billing_additional_info"))
    })
    private Direccion direccionFacturacion;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "subtotal_amount", precision = 10, scale = 2)),
            @AttributeOverride(name = "moneda", column = @Column(name = "subtotal_currency", length = 3))
    })
    private Dinero subtotal;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "monto_impuesto", precision = 10, scale = 2)),
            @AttributeOverride(name = "moneda", column = @Column(name = "tax_currency", length = 3))
    })
    private Dinero montoImpuesto;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "monto_envio", precision = 10, scale = 2)),
            @AttributeOverride(name = "moneda", column = @Column(name = "shipping_currency", length = 3))
    })
    private Dinero montoEnvio;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "total_amount", precision = 10, scale = 2)),
            @AttributeOverride(name = "moneda", column = @Column(name = "total_currency", length = 3))
    })
    private Dinero total;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoOrden estado = EstadoOrden.PENDING;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "ordenes", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ArticuloOrden> articulos = new ArrayList<>();

    @OneToMany(mappedBy = "ordenes", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("creadoEn ASC")
    private List<HistorialEstadoOrden> historialEstados = new ArrayList<>();

    public void confirm() {
        changeStatus(EstadoOrden.CONFIRMED, null);
    }

    public void markAsPaid() {
        changeStatus(EstadoOrden.PAID, null);
    }
    
    public void process() {
        changeStatus(EstadoOrden.PROCESSING, null);
    }

    public void ship() {
        changeStatus(EstadoOrden.SHIPPED, null);
    }

    public void deliver() {
        changeStatus(EstadoOrden.DELIVERED, null);
    }

    public void cancel(String reason) {
        changeStatus(EstadoOrden.CANCELLED, reason);
    }
    
    public void refund(String reason) {
        changeStatus(EstadoOrden.REFUNDED, reason);
    }

    private void changeStatus(EstadoOrden nuevoEstado, String notes) {
        validateTransition(nuevoEstado);
        EstadoOrden estadoAnterior = this.estado;
        this.estado = nuevoEstado;
        
        HistorialEstadoOrden history = new HistorialEstadoOrden();
        history.setIdTienda(this.getIdTienda());
        history.setIdOrden(this.getId());
        history.setEstadoPrevio(estadoAnterior != null ? estadoAnterior.name() : null);
        history.setNuevoEstado(nuevoEstado.name());
        history.setNotes(notes);
        history.setOrdenes(this);
        this.historialEstados.add(history);

        if (this.getId() != null) {
            registerEvent(new EventoEstadoOrdenCambiado(this.getId(), this.getIdTienda(), estadoAnterior, nuevoEstado, notes));
        }
    }

    private void validateTransition(EstadoOrden targetStatus) {
        boolean isValid = switch (this.estado) {
            case PENDING -> targetStatus == EstadoOrden.CONFIRMED || targetStatus == EstadoOrden.CANCELLED;
            case CONFIRMED -> targetStatus == EstadoOrden.PAID || targetStatus == EstadoOrden.CANCELLED;
            case PAID -> targetStatus == EstadoOrden.PROCESSING || targetStatus == EstadoOrden.SHIPPED || targetStatus == EstadoOrden.REFUNDED;
            case PROCESSING -> targetStatus == EstadoOrden.SHIPPED || targetStatus == EstadoOrden.REFUNDED;
            case SHIPPED -> targetStatus == EstadoOrden.DELIVERED || targetStatus == EstadoOrden.REFUNDED;
            case DELIVERED -> targetStatus == EstadoOrden.REFUNDED;
            case CANCELLED, REFUNDED -> false;
        };

        if (!isValid) {
            throw new ExcepcionOperacionInvalida(
                String.format("Cannot transition ordenes %s from %s to %s", 
                    this.numeroOrden != null ? this.numeroOrden : "UNKNOWN", 
                    this.estado, 
                    targetStatus)
            );
        }
    }
}
