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

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "slug", nullable = false)
    private String slug;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "amount", precision = 10, scale = 2)),
            @AttributeOverride(name = "currency", column = @Column(name = "currency", length = 3))
    })
    private Money price;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "compare_at_amount", precision = 10, scale = 2)),
            @AttributeOverride(name = "currency", column = @Column(name = "compare_at_currency", length = 3))
    })
    private Money compareAtPrice;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "cost_amount", precision = 10, scale = 2)),
            @AttributeOverride(name = "currency", column = @Column(name = "cost_currency", length = 3))
    })
    private Money costPrice;

    @Column(name = "sku")
    private String sku;

    @Column(name = "barcode")
    private String barcode;

    @Column(name = "inventory")
    private int inventory = 0;

    @Column(name = "inventory_track_enabled")
    private boolean inventoryTrackEnabled = true;

    @Column(name = "allow_backorder")
    private boolean allowBackorder = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ProductStatus status = ProductStatus.DRAFT;

    @Column(name = "category_id")
    private UUID categoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", insertable = false, updatable = false)
    private Category category;

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductVariant> variants = new ArrayList<>();

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<ProductImage> images = new ArrayList<>();

    public void publish() {
        if (this.status == ProductStatus.ARCHIVED) {
            throw new IllegalStateException("No se puede publicar un producto archivado");
        }
        this.status = ProductStatus.ACTIVE;
    }

    public void archive() {
        this.status = ProductStatus.ARCHIVED;
    }

    public boolean isAvailable() {
        return this.status == ProductStatus.ACTIVE &&
                (!this.inventoryTrackEnabled || this.inventory > 0 || this.allowBackorder);
    }

    public void decreaseInventory(int quantity) {
        if (this.inventoryTrackEnabled) {
            if (this.inventory < quantity && !this.allowBackorder) {
                throw new IllegalStateException("Inventario insuficiente para el producto: " + this.name);
            }
            this.inventory -= quantity;
        }
    }

    public void increaseInventory(int quantity) {
        if (this.inventoryTrackEnabled) {
            this.inventory += quantity;
        }
    }

    public void markAsCreated() {
        registerEvent(new ProductCreatedEvent(
                this.getId(),
                this.getTenantId(),
                this.getName(),
                this.getSlug(),
                this.getDescription(),
                this.getStatus().name()
        ));
    }
}
