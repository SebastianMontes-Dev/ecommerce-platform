package com.ecommerce.modulos.ordenes.domain;

import com.ecommerce.modulos.compartido.domain.EntidadInquilino;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cupones")
@Getter
@Setter
@NoArgsConstructor
public class Cupon extends EntidadInquilino {

    @Column(name = "codigo", nullable = false)
    private String codigo; // Ej: SUMMER20

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoDescuento tipo; 

    @Column(name = "valor", nullable = false)
    private BigDecimal valor; // Ej: 20.00 (Si es PORCENTAJE, es 20%, si es FIJO, es $20)

    @Column(name = "fecha_expiracion")
    private LocalDateTime fechaExpiracion;

    @Column(name = "limite_usos")
    private Integer limiteUsos;

    @Column(name = "usos_actuales", nullable = false)
    private int usosActuales = 0;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    public boolean esValido() {
        if (!activo) return false;
        if (fechaExpiracion != null && LocalDateTime.now().isAfter(fechaExpiracion)) return false;
        if (limiteUsos != null && usosActuales >= limiteUsos) return false;
        return true;
    }

    public void registrarUso() {
        if (!esValido()) {
            throw new IllegalStateException("El cupón no es válido o ha expirado");
        }
        this.usosActuales++;
    }
}
