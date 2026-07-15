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

    @Column(name = "amount", precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "moneda", length = 3)
    private String moneda;

    private Dinero(BigDecimal amount, String moneda) {
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
        this.moneda = moneda;
    }

    public static Dinero of(BigDecimal amount, String moneda) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (moneda == null || moneda.isBlank()) {
            throw new IllegalArgumentException("Currency cannot be null or blank");
        }
        return new Dinero(amount, Currency.getInstance(moneda.toUpperCase()).getCurrencyCode());
    }

    public static Dinero usd(BigDecimal amount) {
        return of(amount, "USD");
    }

    public static Dinero zero(String moneda) {
        return of(BigDecimal.ZERO, moneda);
    }

    public Dinero add(Dinero other) {
        assertSameCurrency(other);
        return new Dinero(this.amount.add(other.amount), this.moneda);
    }

    public Dinero subtract(Dinero other) {
        assertSameCurrency(other);
        return new Dinero(this.amount.subtract(other.amount), this.moneda);
    }

    public Dinero multiply(int multiplier) {
        return new Dinero(this.amount.multiply(BigDecimal.valueOf(multiplier)), this.moneda);
    }

    public Dinero multiply(BigDecimal multiplier) {
        return new Dinero(this.amount.multiply(multiplier), this.moneda);
    }

    public Dinero porcentaje(BigDecimal percent) {
        return new Dinero(this.amount.multiply(percent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP), this.moneda);
    }

    public boolean isGreaterThan(Dinero other) {
        assertSameCurrency(other);
        return this.amount.compareTo(other.amount) > 0;
    }

    public boolean isZero() {
        return BigDecimal.ZERO.compareTo(this.amount) == 0;
    }

    private void assertSameCurrency(Dinero other) {
        if (!this.moneda.equals(other.moneda)) {
            throw new IllegalArgumentException(
                String.format("Currency mismatch: %s vs %s", this.moneda, other.moneda));
        }
    }

    @Override
    public String toString() {
        return String.format("%s %s", moneda, amount);
    }
}
