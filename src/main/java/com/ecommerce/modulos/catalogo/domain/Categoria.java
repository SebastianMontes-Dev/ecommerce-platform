package com.ecommerce.modulos.catalogo.domain;

import com.ecommerce.modulos.compartido.domain.EntidadInquilino;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "categorias")
@Getter
@Setter
@NoArgsConstructor
public class Categoria extends EntidadInquilino {

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "enlace_corto", nullable = false)
    private String enlaceCorto;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "imagen_url")
    private String urlImagen;

    @Column(name = "id_padre")
    private UUID idPadre;

    @Column(name = "active")
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_padre", insertable = false, updatable = false)
    private Categoria parent;

    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    @OrderBy("nombre ASC")
    private List<Categoria> children = new ArrayList<>();

    public boolean isRoot() {
        return idPadre == null;
    }
}
