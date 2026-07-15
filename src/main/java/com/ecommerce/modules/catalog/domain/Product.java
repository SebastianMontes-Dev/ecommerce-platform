package com.ecommerce.modules.catalog.domain;

import com.ecommerce.modules.catalog.domain.events.ProductCreatedEvent;
import com.ecommerce.modules.shared.domain.Money;
import com.ecommerce.modules.shared.domain.TenantAwareAggregateRoot;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
public class Product extends TenantAwareAggregateRoot {

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "slug", nullable = false)
    private String slug;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "amount", precision = 10, scale = 2)),
            @AttributeOverride(name = "currency", column = @Column(name = "currency", length = 3))
    })
    private Money precio;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "monto_comparacion", precision = 10, scale = 2)),
            @AttributeOverride(name = "currency", column = @Column(name = "moneda_comparacion", length = 3))
    })
    private Money precioComparacion;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "monto_costo", precision = 10, scale = 2)),
            @AttributeOverride(name = "currency", column = @Column(name = "moneda_costo", length = 3))
    })
    private Money precioCosto;

    @Column(name = "sku")
    private String sku;

    @Column(name = "codigoBarras")
    private String codigoBarras;

    @Column(name = "inventario")
    private int inventario = 0;

    @Column(name = "rastreo_inventario_habilitado")
    private boolean rastreoInventarioHabilitado = true;

    @Column(name = "permitir_reserva")
    private boolean permitirReserva = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private ProductStatus estado = ProductStatus.DRAFT;

    @Column(name = "category_id")
    private UUID idCategoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", insertable = false, updatable = false)
    private Category category;

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductVariant> variants = new ArrayList<>();

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<ProductImage> images = new ArrayList<>();

    public void publish() {
        if (this.estado == ProductStatus.ARCHIVED) {
            throw new IllegalStateException("No se puede publicar un producto archivado");
        }
        this.estado = ProductStatus.ACTIVE;
    }

    public void archive() {
        this.estado = ProductStatus.ARCHIVED;
    }

    public boolean isAvailable() {
        return this.estado == ProductStatus.ACTIVE &&
                (!this.rastreoInventarioHabilitado || this.inventario > 0 || this.permitirReserva);
    }

    public void decreaseInventory(int cantidad) {
        if (this.rastreoInventarioHabilitado) {
            if (this.inventario < cantidad && !this.permitirReserva) {
                throw new IllegalStateException("Inventario insuficiente para el producto: " + this.nombre);
            }
            this.inventario -= cantidad;
        }
    }

    public void increaseInventory(int cantidad) {
        if (this.rastreoInventarioHabilitado) {
            this.inventario += cantidad;
        }
    }

    public void markAsCreated() {
        registerEvent(new ProductCreatedEvent(
                this.getId(),
                this.getIdTienda(),
                this.getNombre(),
                this.getSlug(),
                this.getDescripcion(),
                this.getEstado().name()
        ));
    }
}
