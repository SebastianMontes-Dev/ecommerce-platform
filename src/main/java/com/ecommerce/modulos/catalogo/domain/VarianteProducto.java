package com.ecommerce.modulos.catalogo.domain;

import com.ecommerce.modulos.compartido.domain.Dinero;
import com.ecommerce.modulos.compartido.domain.EntidadInquilino;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "product_variants")
@Getter
@Setter
@NoArgsConstructor
public class VarianteProducto extends EntidadInquilino {

    @Column(name = "product_id", nullable = false)
    private UUID idProducto;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "sku")
    private String sku;

    @Column(name = "monto")
    private BigDecimal monto;

    @Column(name = "moneda")
    private String moneda = "USD";

    @Column(name = "inventario")
    private int inventario = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attributes", columnDefinition = "jsonb")
    private Map<String, String> attributes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    private Producto producto;

    public Dinero getPrecio() {
        if (monto != null && moneda != null) {
            return Dinero.of(monto, moneda);
        }
        return producto != null ? producto.getPrecio() : null;
    }

    public void decreaseInventory(int cantidad) {
        if (this.inventario < cantidad) {
            throw new IllegalStateException("Insufficient inventario for variant: " + this.nombre);
        }
        this.inventario -= cantidad;
    }
}
