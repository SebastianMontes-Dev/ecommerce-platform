package com.ecommerce.modules.tenant.domain;

import com.ecommerce.modules.shared.domain.*;
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
public class Subscription extends TenantAwareEntity {

    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    @Column(name = "status", nullable = false)
    private String status = "ACTIVE";

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", insertable = false, updatable = false)
    private SubscriptionPlan plan;

    public boolean isActive() {
        return "ACTIVE".equals(status) &&
                (endDate == null || endDate.isAfter(LocalDateTime.now()));
    }

    public void cancel() {
        this.status = "CANCELLED";
        this.endDate = LocalDateTime.now();
    }
}
