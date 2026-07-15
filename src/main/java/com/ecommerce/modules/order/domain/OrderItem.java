package com.ecommerce.modules.order.domain;

import com.ecommerce.modules.shared.domain.BaseEntity;
import com.ecommerce.modules.shared.domain.Money;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
public class OrderItem extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID idTienda;

    @Column(name = "order_id", nullable = false)
    private UUID idOrden;

    @Column(name = "product_id", nullable = false)
    private UUID idProducto;

    @Column(name = "product_name", nullable = false)
    private String nombreProducto;

    @Column(name = "variant_id")
    private UUID variantId;

    @Column(name = "variant_name")
    private String variantName;

    @Column(name = "cantidad", nullable = false)
    private int cantidad;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "unit_price_amount", precision = 10, scale = 2)),
            @AttributeOverride(name = "currency", column = @Column(name = "unit_price_currency", length = 3))
    })
    private Money unitPrice;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "subtotal_amount", precision = 10, scale = 2)),
            @AttributeOverride(name = "currency", column = @Column(name = "subtotal_currency", length = 3))
    })
    private Money subtotal;

    @Column(name = "image_url")
    private String urlImagen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", insertable = false, updatable = false)
    private Order order;
}
