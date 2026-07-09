package com.ecommerce.modules.shared.domain;

import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class Quantity {

    private int value;

    private Quantity(int value) {
        if (value < 1) throw new IllegalArgumentException("Quantity must be greater than 0, got: " + value);
        this.value = value;
    }

    public static Quantity of(int value) {
        return new Quantity(value);
    }

    public Quantity add(Quantity other) {
        return new Quantity(this.value + other.value);
    }

    public Quantity subtract(Quantity other) {
        int result = this.value - other.value;
        if (result < 0) throw new IllegalArgumentException("Quantity cannot go below 0");
        return new Quantity(result);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
