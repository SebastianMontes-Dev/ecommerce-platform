package com.ecommerce.modules.tenant.domain;

import com.ecommerce.modules.shared.domain.*;
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
@Table(name = "subscription_plans")
@Getter
@Setter
@NoArgsConstructor
public class SubscriptionPlan extends BaseEntity {

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_plan", nullable = false)
    private SubscriptionPlanType tipoPlan;

    @Column(name = "precio", nullable = false)
    private BigDecimal precio;

    @Column(name = "max_products", nullable = false)
    private int maxProducts;

    @Column(name = "tasa_comision", nullable = false)
    private BigDecimal tasaComision;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "features", columnDefinition = "jsonb")
    private Map<String, Boolean> features;

    @Column(name = "active")
    private boolean active = true;

    public Percentage getCommissionPercentage() {
        return Percentage.of(tasaComision);
    }

    public boolean hasFeature(String feature) {
        return features != null && features.getOrDefault(feature, false);
    }
}
