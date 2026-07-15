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
@Table(name = "reviews", uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "id_cliente", "order_id"}))
@Getter
@Setter
@NoArgsConstructor
public class Review extends TenantAwareEntity {

    @Column(name = "product_id", nullable = false)
    private UUID idProducto;

    @Column(name = "id_cliente", nullable = false)
    private UUID idCliente;

    @Column(name = "order_id", nullable = false)
    private UUID idOrden;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "value", precision = 2, scale = 1))
    private Rating rating;

    @Column(name = "title")
    private String titulo;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comentario;

    @Column(name = "active")
    private boolean activo = true;

    public void hide() {
        this.activo = false;
    }

    public void show() {
        this.activo = true;
    }
}
