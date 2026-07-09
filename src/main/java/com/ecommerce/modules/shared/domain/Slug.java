package com.ecommerce.modules.shared.domain;

import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.regex.Pattern;

@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class Slug {

    private static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9]+(?:-[a-z0-9]+)*$");

    private String value;

    private Slug(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Slug cannot be null or blank");
        if (!SLUG_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid slug format: " + value);
        }
        this.value = value;
    }

    public static Slug of(String value) {
        return new Slug(value);
    }

    public static Slug fromText(String text) {
        if (text == null || text.isBlank()) throw new IllegalArgumentException("Text cannot be null or blank");
        String slug = text.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        return new Slug(slug);
    }

    @Override
    public String toString() {
        return value;
    }
}
