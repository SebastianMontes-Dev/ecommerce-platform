package com.ecommerce.modulos.inquilino.domain;

import com.ecommerce.modulos.compartido.domain.EntidadAuditableBase;
import com.ecommerce.modulos.compartido.domain.Imagen;
import com.ecommerce.modulos.compartido.domain.EnlaceCorto;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "inquilinos")
@Getter
@Setter
@NoArgsConstructor
public class Inquilino extends EntidadAuditableBase {

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "enlace_corto", nullable = false, unique = true)
    private String enlaceCorto;

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
    private EstadoInquilino estado = EstadoInquilino.TRIAL;

    @Column(name = "id_propietario", nullable = false)
    private UUID idPropietario;

    public Inquilino(String nombre, String enlaceCorto, UUID idPropietario) {
        this.nombre = nombre;
        this.enlaceCorto = EnlaceCorto.of(enlaceCorto).getValue();
        this.idPropietario = idPropietario;
        this.estado = EstadoInquilino.TRIAL;
    }

    public void suspend() {
        if (this.estado == EstadoInquilino.CANCELLED) {
            throw new IllegalStateException("Cannot suspend a cancelled inquilino");
        }
        this.estado = EstadoInquilino.SUSPENDED;
    }

    public void activate() {
        if (this.estado == EstadoInquilino.CANCELLED) {
            throw new IllegalStateException("Cannot activate a cancelled inquilino");
        }
        this.estado = EstadoInquilino.ACTIVE;
    }

    public void cancel() {
        this.estado = EstadoInquilino.CANCELLED;
    }

    public boolean isActive() {
        return this.estado == EstadoInquilino.ACTIVE || this.estado == EstadoInquilino.TRIAL;
    }

    public boolean isOwnedBy(UUID userId) {
        return this.idPropietario.equals(userId);
    }

    public Imagen getLogo() {
        if (urlLogo == null) return null;
        return Imagen.of(urlLogo, textoAlternativoLogo != null ? textoAlternativoLogo : nombre + " logo");
    }

    public Imagen getBanner() {
        if (urlBanner == null) return null;
        return Imagen.of(urlBanner, textoAlternativoBanner != null ? textoAlternativoBanner : nombre + " banner");
    }
}
