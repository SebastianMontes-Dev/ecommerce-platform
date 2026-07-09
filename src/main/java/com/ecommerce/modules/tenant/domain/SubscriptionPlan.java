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

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_type", nullable = false)
    private SubscriptionPlanType planType;

    @Column(name = "price", nullable = false)
    private BigDecimal price;

    @Column(name = "max_products", nullable = false)
    private int maxProducts;

    @Column(name = "commission_rate", nullable = false)
    private BigDecimal commissionRate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "features", columnDefinition = "jsonb")
    private Map<String, Boolean> features;

    @Column(name = "active")
    private boolean active = true;

    public Percentage getCommissionPercentage() {
        return Percentage.of(commissionRate);
    }

    public boolean hasFeature(String feature) {
        return features != null && features.getOrDefault(feature, false);
    }
}
