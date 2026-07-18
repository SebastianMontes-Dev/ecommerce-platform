package com.ecommerce.modulos.catalogo.domain;

import com.ecommerce.modulos.catalogo.domain.eventos.EventoProductoCreado;
import com.ecommerce.modulos.compartido.domain.Dinero;
import com.ecommerce.modulos.compartido.domain.RaizAgregadaInquilino;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "productos")
@Getter
@Setter
@NoArgsConstructor
public class Producto extends RaizAgregadaInquilino {

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "enlace_corto", nullable = false)
    private String enlaceCorto;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "monto", column = @Column(name = "monto", precision = 10, scale = 2)),
            @AttributeOverride(name = "moneda", column = @Column(name = "moneda", length = 3))
    })
    private Dinero precio;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "monto", column = @Column(name = "monto_comparacion", precision = 10, scale = 2)),
            @AttributeOverride(name = "moneda", column = @Column(name = "moneda_comparacion", length = 3))
    })
    private Dinero precioComparacion;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "monto", column = @Column(name = "monto_costo", precision = 10, scale = 2)),
            @AttributeOverride(name = "moneda", column = @Column(name = "moneda_costo", length = 3))
    })
    private Dinero precioCosto;

    @Column(name = "sku")
    private String sku;

    @Column(name = "codigo_barras")
    private String codigoBarras;

    @Column(name = "inventario")
    private int inventario = 0;

    @Column(name = "rastreo_inventario_habilitado")
    private boolean rastreoInventarioHabilitado = true;

    @Column(name = "permitir_reserva")
    private boolean permitirReserva = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoProducto estado = EstadoProducto.DRAFT;

    @Column(name = "category_id")
    private UUID idCategoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", insertable = false, updatable = false)
    private Categoria categoria;

    @OneToMany(mappedBy = "producto", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VarianteProducto> variants = new ArrayList<>();

    @OneToMany(mappedBy = "producto", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<ImagenProducto> images = new ArrayList<>();

    public void publish() {
        if (this.estado == EstadoProducto.ARCHIVED) {
            throw new IllegalStateException("No se puede publicar un producto archivado");
        }
        this.estado = EstadoProducto.ACTIVE;
    }

    public void archive() {
        this.estado = EstadoProducto.ARCHIVED;
    }

    public boolean isAvailable() {
        return this.estado == EstadoProducto.ACTIVE &&
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
        registerEvent(new EventoProductoCreado(
                this.getId(),
                this.getIdTienda(),
                this.getNombre(),
                this.getEnlaceCorto(),
                this.getDescripcion(),
                this.getEstado().name()
        ));
    }
}
