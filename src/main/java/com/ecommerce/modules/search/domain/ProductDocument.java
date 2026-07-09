package com.ecommerce.modules.search.domain;

import java.math.BigDecimal;
import java.util.UUID;

public class ProductDocument {

    private String id;
    private UUID tenantId;
    private String name;
    private String description;
    private String slug;
    private BigDecimal price;
    private String categoryName;
    private String imageUrl;
    private double averageRating;
    private long reviewCount;

    public ProductDocument() {}

    public ProductDocument(UUID productId, UUID tenantId, String name, String description,
                           String slug, BigDecimal price, String categoryName) {
        this.id = productId.toString();
        this.tenantId = tenantId;
        this.name = name;
        this.description = description;
        this.slug = slug;
        this.price = price;
        this.categoryName = categoryName;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public double getAverageRating() { return averageRating; }
    public void setAverageRating(double averageRating) { this.averageRating = averageRating; }
    public long getReviewCount() { return reviewCount; }
    public void setReviewCount(long reviewCount) { this.reviewCount = reviewCount; }
}
