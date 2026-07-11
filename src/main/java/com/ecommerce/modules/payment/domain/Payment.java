package com.ecommerce.modules.payment.domain;

import com.ecommerce.modules.shared.domain.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
public class Payment extends TenantAwareEntity {

    @Column(name = "order_id", nullable = false)
    private UUID idOrden;

    @Column(name = "amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency = "USD";

    @Column(name = "payment_method", length = 20, nullable = false)
    private String paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 20, nullable = false)
    private PaymentStatus estado = PaymentStatus.PENDING;

    @Column(name = "external_id")
    private String externalId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    public void complete() {
        this.estado = PaymentStatus.COMPLETED;
    }

    public void fail() {
        this.estado = PaymentStatus.FAILED;
    }
}
