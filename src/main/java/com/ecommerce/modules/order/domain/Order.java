package com.ecommerce.modules.order.domain;

import com.ecommerce.modules.shared.domain.*;
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
public class Order extends TenantAwareEntity {

    @Column(name = "order_number", unique = true, nullable = false, length = 100)
    private String orderNumber;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "customer_email", nullable = false)
    private String customerEmail;

    @Column(name = "customer_name")
    private String customerName;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "street", column = @Column(name = "shipping_street")),
            @AttributeOverride(name = "city", column = @Column(name = "shipping_city")),
            @AttributeOverride(name = "state", column = @Column(name = "shipping_state")),
            @AttributeOverride(name = "zipCode", column = @Column(name = "shipping_zip_code")),
            @AttributeOverride(name = "country", column = @Column(name = "shipping_country")),
            @AttributeOverride(name = "additionalInfo", column = @Column(name = "shipping_additional_info"))
    })
    private Address shippingAddress;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "street", column = @Column(name = "billing_street")),
            @AttributeOverride(name = "city", column = @Column(name = "billing_city")),
            @AttributeOverride(name = "state", column = @Column(name = "billing_state")),
            @AttributeOverride(name = "zipCode", column = @Column(name = "billing_zip_code")),
            @AttributeOverride(name = "country", column = @Column(name = "billing_country")),
            @AttributeOverride(name = "additionalInfo", column = @Column(name = "billing_additional_info"))
    })
    private Address billingAddress;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "subtotal_amount", precision = 10, scale = 2)),
            @AttributeOverride(name = "currency", column = @Column(name = "subtotal_currency", length = 3))
    })
    private Money subtotal;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "tax_amount", precision = 10, scale = 2)),
            @AttributeOverride(name = "currency", column = @Column(name = "tax_currency", length = 3))
    })
    private Money taxAmount;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "shipping_amount", precision = 10, scale = 2)),
            @AttributeOverride(name = "currency", column = @Column(name = "shipping_currency", length = 3))
    })
    private Money shippingAmount;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "total_amount", precision = 10, scale = 2)),
            @AttributeOverride(name = "currency", column = @Column(name = "total_currency", length = 3))
    })
    private Money total;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<OrderStatusHistory> statusHistory = new ArrayList<>();

    public void confirm() {
        validateTransition(OrderStatus.CONFIRMED);
        this.status = OrderStatus.CONFIRMED;
    }

    public void markAsPaid() {
        validateTransition(OrderStatus.PAID);
        this.status = OrderStatus.PAID;
    }
    
    public void process() {
        validateTransition(OrderStatus.PROCESSING);
        this.status = OrderStatus.PROCESSING;
    }

    public void ship() {
        validateTransition(OrderStatus.SHIPPED);
        this.status = OrderStatus.SHIPPED;
    }

    public void deliver() {
        validateTransition(OrderStatus.DELIVERED);
        this.status = OrderStatus.DELIVERED;
    }

    public void cancel(String reason) {
        validateTransition(OrderStatus.CANCELLED);
        this.status = OrderStatus.CANCELLED;
    }
    
    public void refund(String reason) {
        validateTransition(OrderStatus.REFUNDED);
        this.status = OrderStatus.REFUNDED;
    }

    private void validateTransition(OrderStatus targetStatus) {
        boolean isValid = switch (this.status) {
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
                    this.orderNumber != null ? this.orderNumber : "UNKNOWN", 
                    this.status, 
                    targetStatus)
            );
        }
    }
}
