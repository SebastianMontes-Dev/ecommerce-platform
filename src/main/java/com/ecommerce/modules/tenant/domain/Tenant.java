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

    @Column(name = "url_logo")
    private String urlLogo;

    @Column(name = "texto_alternativo_logo")
    private String textoAlternativoLogo;

    @Column(name = "url_banner")
    private String urlBanner;

    @Column(name = "texto_alternativo_banner")
    private String textoAlternativoBanner;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private TenantStatus estado = TenantStatus.TRIAL;

    @Column(name = "id_propietario", nullable = false)
    private UUID idPropietario;

    public Tenant(String nombre, String slug, UUID idPropietario) {
        this.nombre = nombre;
        this.slug = Slug.of(slug).getValue();
        this.idPropietario = idPropietario;
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
        return this.idPropietario.equals(userId);
    }

    public Image getLogo() {
        if (urlLogo == null) return null;
        return Image.of(urlLogo, textoAlternativoLogo != null ? textoAlternativoLogo : nombre + " logo");
    }

    public Image getBanner() {
        if (urlBanner == null) return null;
        return Image.of(urlBanner, textoAlternativoBanner != null ? textoAlternativoBanner : nombre + " banner");
    }
}
