package com.ecommerce.modulos.compartido.domain;

import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class Calificacion {

    private BigDecimal value;

    private Calificacion(BigDecimal value) {
        if (value == null) throw new IllegalArgumentException("Calificacion value cannot be null");
        if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(BigDecimal.valueOf(5)) > 0) {
            throw new IllegalArgumentException("Calificacion must be between 0 and 5, got: " + value);
        }
        this.value = value.setScale(1, RoundingMode.HALF_UP);
    }

    public static Calificacion of(BigDecimal value) {
        return new Calificacion(value);
    }

    public static Calificacion of(int value) {
        return new Calificacion(BigDecimal.valueOf(value));
    }

    public static Calificacion zero() {
        return new Calificacion(BigDecimal.ZERO);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
