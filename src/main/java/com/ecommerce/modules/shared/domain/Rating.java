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
public class Rating {

    private BigDecimal value;

    private Rating(BigDecimal value) {
        if (value == null) throw new IllegalArgumentException("Rating value cannot be null");
        if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(BigDecimal.valueOf(5)) > 0) {
            throw new IllegalArgumentException("Rating must be between 0 and 5, got: " + value);
        }
        this.value = value.setScale(1, RoundingMode.HALF_UP);
    }

    public static Rating of(BigDecimal value) {
        return new Rating(value);
    }

    public static Rating of(int value) {
        return new Rating(BigDecimal.valueOf(value));
    }

    public static Rating zero() {
        return new Rating(BigDecimal.ZERO);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
