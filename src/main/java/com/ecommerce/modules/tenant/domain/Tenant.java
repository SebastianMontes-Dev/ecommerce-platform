package com.ecommerce.modules.tenant.domain;

import com.ecommerce.modules.shared.domain.BaseAuditableEntity;
import com.ecommerce.modules.shared.domain.Image;
import com.ecommerce.modules.shared.domain.Slug;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "tenants")
@Getter
@Setter
@NoArgsConstructor
public class Tenant extends BaseAuditableEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "slug", nullable = false, unique = true)
    private String slug;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "logo_alt_text")
    private String logoAltText;

    @Column(name = "banner_url")
    private String bannerUrl;

    @Column(name = "banner_alt_text")
    private String bannerAltText;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TenantStatus status = TenantStatus.TRIAL;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    public Tenant(String name, String slug, UUID ownerId) {
        this.name = name;
        this.slug = Slug.of(slug).getValue();
        this.ownerId = ownerId;
        this.status = TenantStatus.TRIAL;
    }

    public void suspend() {
        if (this.status == TenantStatus.CANCELLED) {
            throw new IllegalStateException("Cannot suspend a cancelled tenant");
        }
        this.status = TenantStatus.SUSPENDED;
    }

    public void activate() {
        if (this.status == TenantStatus.CANCELLED) {
            throw new IllegalStateException("Cannot activate a cancelled tenant");
        }
        this.status = TenantStatus.ACTIVE;
    }

    public void cancel() {
        this.status = TenantStatus.CANCELLED;
    }

    public boolean isActive() {
        return this.status == TenantStatus.ACTIVE || this.status == TenantStatus.TRIAL;
    }

    public boolean isOwnedBy(UUID userId) {
        return this.ownerId.equals(userId);
    }

    public Image getLogo() {
        if (logoUrl == null) return null;
        return Image.of(logoUrl, logoAltText != null ? logoAltText : name + " logo");
    }

    public Image getBanner() {
        if (bannerUrl == null) return null;
        return Image.of(bannerUrl, bannerAltText != null ? bannerAltText : name + " banner");
    }
}
