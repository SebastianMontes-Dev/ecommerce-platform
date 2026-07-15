package com.ecommerce.modules.catalog.domain;

import com.ecommerce.modules.shared.domain.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
public class Category extends TenantAwareEntity {

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "slug", nullable = false)
    private String slug;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "image_url")
    private String urlImagen;

    @Column(name = "id_padre")
    private UUID idPadre;

    @Column(name = "active")
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_padre", insertable = false, updatable = false)
    private Category parent;

    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    @OrderBy("nombre ASC")
    private List<Category> children = new ArrayList<>();

    public boolean isRoot() {
        return idPadre == null;
    }
}
