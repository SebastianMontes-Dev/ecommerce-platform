package com.ecommerce.modulos.pagos.domain;

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
@Table(name = "pagos")
@Getter
@Setter
@NoArgsConstructor
public class Pago extends EntidadInquilino {

    @Column(name = "order_id", nullable = false)
    private UUID idOrden;

    @Column(name = "monto", precision = 10, scale = 2, nullable = false)
    private BigDecimal monto;

    @Column(name = "moneda", length = 3, nullable = false)
    private String moneda = "USD";

    @Column(name = "metodo_pago", length = 20, nullable = false)
    private String metodoPago;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 20, nullable = false)
    private EstadoPago estado = EstadoPago.PENDING;

    @Column(name = "id_externo")
    private String idExterno;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    public void complete() {
        this.estado = EstadoPago.COMPLETED;
    }

    public void fail() {
        this.estado = EstadoPago.FAILED;
    }
}
