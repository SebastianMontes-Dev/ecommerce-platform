package com.ecommerce.modulos.compartido.domain;

import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class Cantidad {

    private int value;

    private Cantidad(int value) {
        if (value < 1) throw new IllegalArgumentException("Cantidad must be greater than 0, got: " + value);
        this.value = value;
    }

    public static Cantidad of(int value) {
        return new Cantidad(value);
    }

    public Cantidad add(Cantidad other) {
        return new Cantidad(this.value + other.value);
    }

    public Cantidad subtract(Cantidad other) {
        int result = this.value - other.value;
        if (result < 0) throw new IllegalArgumentException("Cantidad cannot go below 0");
        return new Cantidad(result);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
