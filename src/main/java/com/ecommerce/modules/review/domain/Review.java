package com.ecommerce.modules.review.domain;

import com.ecommerce.modules.shared.domain.TenantAwareEntity;
import com.ecommerce.modules.shared.domain.Rating;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "reviews", uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "customer_id", "order_id"}))
@Getter
@Setter
@NoArgsConstructor
public class Review extends TenantAwareEntity {

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "value", precision = 2, scale = 1))
    private Rating rating;

    @Column(name = "title")
    private String title;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "active")
    private boolean active = true;

    public void hide() {
        this.active = false;
    }

    public void show() {
        this.active = true;
    }
}
