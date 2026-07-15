package com.ecommerce.modulos.compartido.domain;

import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.regex.Pattern;

@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class EnlaceCorto {

    private static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9]+(?:-[a-z0-9]+)*$");

    private String value;

    private EnlaceCorto(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("EnlaceCorto cannot be null or blank");
        if (!SLUG_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid enlaceCorto format: " + value);
        }
        this.value = value;
    }

    public static EnlaceCorto of(String value) {
        return new EnlaceCorto(value);
    }

    public static EnlaceCorto fromText(String text) {
        if (text == null || text.isBlank()) throw new IllegalArgumentException("Text cannot be null or blank");
        String enlaceCorto = text.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        return new EnlaceCorto(enlaceCorto);
    }

    @Override
    public String toString() {
        return value;
    }
}
