package com.ecommerce.modulos.compartido.domain;

import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class Imagen {

    private String url;
    private String altText;
    private Integer width;
    private Integer height;

    private Imagen(String url, String altText, Integer width, Integer height) {
        this.url = url;
        this.altText = altText;
        this.width = width;
        this.height = height;
    }

    public static Imagen of(String url, String altText) {
        if (url == null || url.isBlank()) throw new IllegalArgumentException("Imagen URL is required");
        return new Imagen(url, altText, null, null);
    }

    public static Imagen of(String url, String altText, Integer width, Integer height) {
        if (url == null || url.isBlank()) throw new IllegalArgumentException("Imagen URL is required");
        return new Imagen(url, altText, width, height);
    }

    @Override
    public String toString() {
        return url;
    }
}
