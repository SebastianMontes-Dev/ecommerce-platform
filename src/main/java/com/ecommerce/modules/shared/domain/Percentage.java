package com.ecommerce.modules.shared.domain;

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
public class Percentage {

    private BigDecimal value;

    private Percentage(BigDecimal value) {
        if (value == null) throw new IllegalArgumentException("Percentage value cannot be null");
        if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Percentage must be between 0 and 100, got: " + value);
        }
        this.value = value.setScale(2, RoundingMode.HALF_UP);
    }

    public static Percentage of(BigDecimal value) {
        return new Percentage(value);
    }

    public static Percentage of(double value) {
        return new Percentage(BigDecimal.valueOf(value));
    }

    public static Percentage zero() {
        return new Percentage(BigDecimal.ZERO);
    }

    public Money applyTo(Money money) {
        return money.percentage(value);
    }

    @Override
    public String toString() {
        return value + "%";
    }
}
