package com.ecommerce.modulos.compartido.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class Dinero {

    @Column(name = "monto", precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(name = "moneda", length = 3)
    private String moneda;

    private Dinero(BigDecimal monto, String moneda) {
        this.monto = monto.setScale(2, RoundingMode.HALF_UP);
        this.moneda = moneda;
    }

    public static Dinero of(BigDecimal monto, String moneda) {
        if (monto == null) {
            throw new IllegalArgumentException("Monto cannot be null");
        }
        if (moneda == null || moneda.isBlank()) {
            throw new IllegalArgumentException("Currency cannot be null or blank");
        }
        return new Dinero(monto, Currency.getInstance(moneda.toUpperCase()).getCurrencyCode());
    }

    public static Dinero usd(BigDecimal monto) {
        return of(monto, "USD");
    }

    public static Dinero zero(String moneda) {
        return of(BigDecimal.ZERO, moneda);
    }

    public Dinero add(Dinero other) {
        assertSameCurrency(other);
        return new Dinero(this.monto.add(other.monto), this.moneda);
    }

    public Dinero subtract(Dinero other) {
        assertSameCurrency(other);
        return new Dinero(this.monto.subtract(other.monto), this.moneda);
    }

    public Dinero multiply(int multiplier) {
        return new Dinero(this.monto.multiply(BigDecimal.valueOf(multiplier)), this.moneda);
    }

    public Dinero multiply(BigDecimal multiplier) {
        return new Dinero(this.monto.multiply(multiplier), this.moneda);
    }

    public Dinero porcentaje(BigDecimal percent) {
        return new Dinero(this.monto.multiply(percent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP), this.moneda);
    }

    public boolean isGreaterThan(Dinero other) {
        assertSameCurrency(other);
        return this.monto.compareTo(other.monto) > 0;
    }

    public boolean isZero() {
        return BigDecimal.ZERO.compareTo(this.monto) == 0;
    }

    private void assertSameCurrency(Dinero other) {
        if (!this.moneda.equals(other.moneda)) {
            throw new IllegalArgumentException(
                String.format("Currency mismatch: %s vs %s", this.moneda, other.moneda));
        }
    }

    @Override
    public String toString() {
        return String.format("%s %s", moneda, monto);
    }
}
