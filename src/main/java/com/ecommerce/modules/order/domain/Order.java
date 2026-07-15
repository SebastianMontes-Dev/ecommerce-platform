package com.ecommerce.modules.order.domain;

import com.ecommerce.modules.shared.domain.*;
import com.ecommerce.modules.order.domain.events.OrderStatusChangedEvent;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
public class Order extends TenantAwareAggregateRoot {

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
    private Address direccionEnvio;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "street", column = @Column(name = "billing_street")),
            @AttributeOverride(name = "city", column = @Column(name = "billing_city")),
            @AttributeOverride(name = "state", column = @Column(name = "billing_state")),
            @AttributeOverride(name = "codigoPostal", column = @Column(name = "billing_zip_code")),
            @AttributeOverride(name = "country", column = @Column(name = "billing_country")),
            @AttributeOverride(name = "additionalInfo", column = @Column(name = "billing_additional_info"))
    })
    private Address direccionFacturacion;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "subtotal_amount", precision = 10, scale = 2)),
            @AttributeOverride(name = "currency", column = @Column(name = "subtotal_currency", length = 3))
    })
    private Money subtotal;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "monto_impuesto", precision = 10, scale = 2)),
            @AttributeOverride(name = "currency", column = @Column(name = "tax_currency", length = 3))
    })
    private Money montoImpuesto;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "monto_envio", precision = 10, scale = 2)),
            @AttributeOverride(name = "currency", column = @Column(name = "shipping_currency", length = 3))
    })
    private Money montoEnvio;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "total_amount", precision = 10, scale = 2)),
            @AttributeOverride(name = "currency", column = @Column(name = "total_currency", length = 3))
    })
    private Money total;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private OrderStatus estado = OrderStatus.PENDING;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("creadoEn ASC")
    private List<OrderHistorialEstados> historialEstados = new ArrayList<>();

    public void confirm() {
        changeStatus(OrderStatus.CONFIRMED, null);
    }

    public void markAsPaid() {
        changeStatus(OrderStatus.PAID, null);
    }
    
    public void process() {
        changeStatus(OrderStatus.PROCESSING, null);
    }

    public void ship() {
        changeStatus(OrderStatus.SHIPPED, null);
    }

    public void deliver() {
        changeStatus(OrderStatus.DELIVERED, null);
    }

    public void cancel(String reason) {
        changeStatus(OrderStatus.CANCELLED, reason);
    }
    
    public void refund(String reason) {
        changeStatus(OrderStatus.REFUNDED, reason);
    }

    private void changeStatus(OrderStatus nuevoEstado, String notes) {
        validateTransition(nuevoEstado);
        OrderStatus estadoAnterior = this.estado;
        this.estado = nuevoEstado;
        
        OrderHistorialEstados history = new OrderHistorialEstados();
        history.setIdTienda(this.getIdTienda());
        history.setIdOrden(this.getId());
        history.setEstadoPrevio(estadoAnterior != null ? estadoAnterior.nombre() : null);
        history.setNuevoEstado(nuevoEstado.nombre());
        history.setNotes(notes);
        history.setOrder(this);
        this.historialEstados.add(history);

        if (this.getId() != null) {
            registerEvent(new OrderStatusChangedEvent(this.getId(), this.getIdTienda(), estadoAnterior, nuevoEstado, notes));
        }
    }

    private void validateTransition(OrderStatus targetStatus) {
        boolean isValid = switch (this.estado) {
            case PENDING -> targetStatus == OrderStatus.CONFIRMED || targetStatus == OrderStatus.CANCELLED;
            case CONFIRMED -> targetStatus == OrderStatus.PAID || targetStatus == OrderStatus.CANCELLED;
            case PAID -> targetStatus == OrderStatus.PROCESSING || targetStatus == OrderStatus.SHIPPED || targetStatus == OrderStatus.REFUNDED;
            case PROCESSING -> targetStatus == OrderStatus.SHIPPED || targetStatus == OrderStatus.REFUNDED;
            case SHIPPED -> targetStatus == OrderStatus.DELIVERED || targetStatus == OrderStatus.REFUNDED;
            case DELIVERED -> targetStatus == OrderStatus.REFUNDED;
            case CANCELLED, REFUNDED -> false;
        };

        if (!isValid) {
            throw new InvalidOperationException(
                String.format("Cannot transition order %s from %s to %s", 
                    this.numeroOrden != null ? this.numeroOrden : "UNKNOWN", 
                    this.estado, 
                    targetStatus)
            );
        }
    }
}
