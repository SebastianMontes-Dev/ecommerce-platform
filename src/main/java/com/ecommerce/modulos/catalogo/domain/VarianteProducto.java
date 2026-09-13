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
import java.util.HashMap;
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

    // La columna es NOT NULL DEFAULT '{}' (V4__create_catalog.sql), pero Hibernate siempre
    // manda el valor explícito del campo en el INSERT (nunca deja que la DB aplique su
    // default) — sin este default en memoria, un caller que no setea attributes viola la
    // constraint NOT NULL en cualquier entorno con Postgres real.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attributes", columnDefinition = "jsonb")
    private Map<String, String> attributes = new HashMap<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    private Producto producto;

    public Dinero getPrecio() {
        if (monto != null && moneda != null) {
            return Dinero.of(monto, moneda);
        }
        return producto != null ? producto.getPrecio() : null;
    }

    /**
     * Descuenta inventario de la variante. Igual que en {@link Producto}, debe correr
     * sobre una fila bloqueada con {@code findByIdForUpdate}.
     */
    public void decreaseInventory(int cantidad) {
        if (this.inventario < cantidad) {
            throw new com.ecommerce.modulos.compartido.domain.ExcepcionStockInsuficiente(
                    "Inventario insuficiente para la variante: " + this.nombre);
        }
        this.inventario -= cantidad;
    }

    public void increaseInventory(int cantidad) {
        this.inventario += cantidad;
    }
}
