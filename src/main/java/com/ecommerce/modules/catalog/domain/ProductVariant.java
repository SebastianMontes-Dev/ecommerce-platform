package com.ecommerce.modules.catalog.domain;

import com.ecommerce.modules.shared.domain.Money;
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
@Table(name = "product_variants")
@Getter
@Setter
@NoArgsConstructor
public class ProductVariant extends TenantAwareEntity {

    @Column(name = "product_id", nullable = false)
    private UUID idProducto;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "sku")
    private String sku;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "currency")
    private String currency = "USD";

    @Column(name = "inventario")
    private int inventario = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attributes", columnDefinition = "jsonb")
    private Map<String, String> attributes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    private Product product;

    public Money getPrecio() {
        if (amount != null && currency != null) {
            return Money.of(amount, currency);
        }
        return product != null ? product.getPrecio() : null;
    }

    public void decreaseInventory(int cantidad) {
        if (this.inventario < cantidad) {
            throw new IllegalStateException("Insufficient inventario for variant: " + this.nombre);
        }
        this.inventario -= cantidad;
    }
}
