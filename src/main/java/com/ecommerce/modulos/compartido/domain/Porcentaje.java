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
public class Porcentaje {

    private BigDecimal value;

    private Porcentaje(BigDecimal value) {
        if (value == null) throw new IllegalArgumentException("Porcentaje value cannot be null");
        if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Porcentaje must be between 0 and 100, got: " + value);
        }
        this.value = value.setScale(2, RoundingMode.HALF_UP);
    }

    public static Porcentaje of(BigDecimal value) {
        return new Porcentaje(value);
    }

    public static Porcentaje of(double value) {
        return new Porcentaje(BigDecimal.valueOf(value));
    }

    public static Porcentaje zero() {
        return new Porcentaje(BigDecimal.ZERO);
    }

    public Dinero applyTo(Dinero dinero) {
        return dinero.porcentaje(value);
    }

    @Override
    public String toString() {
        return value + "%";
    }
}
