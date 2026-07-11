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

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "slug", nullable = false, unique = true)
    private String slug;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "logo_alt_text")
    private String logoAltText;

    @Column(name = "banner_url")
    private String bannerUrl;

    @Column(name = "banner_alt_text")
    private String bannerAltText;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private TenantStatus estado = TenantStatus.TRIAL;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    public Tenant(String nombre, String slug, UUID ownerId) {
        this.nombre = nombre;
        this.slug = Slug.of(slug).getValue();
        this.ownerId = ownerId;
        this.estado = TenantStatus.TRIAL;
    }

    public void suspend() {
        if (this.estado == TenantStatus.CANCELLED) {
            throw new IllegalStateException("Cannot suspend a cancelled tenant");
        }
        this.estado = TenantStatus.SUSPENDED;
    }

    public void activate() {
        if (this.estado == TenantStatus.CANCELLED) {
            throw new IllegalStateException("Cannot activate a cancelled tenant");
        }
        this.estado = TenantStatus.ACTIVE;
    }

    public void cancel() {
        this.estado = TenantStatus.CANCELLED;
    }

    public boolean isActive() {
        return this.estado == TenantStatus.ACTIVE || this.estado == TenantStatus.TRIAL;
    }

    public boolean isOwnedBy(UUID userId) {
        return this.ownerId.equals(userId);
    }

    public Image getLogo() {
        if (logoUrl == null) return null;
        return Image.of(logoUrl, logoAltText != null ? logoAltText : nombre + " logo");
    }

    public Image getBanner() {
        if (bannerUrl == null) return null;
        return Image.of(bannerUrl, bannerAltText != null ? bannerAltText : nombre + " banner");
    }
}
