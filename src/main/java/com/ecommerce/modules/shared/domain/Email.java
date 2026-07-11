package com.ecommerce.modules.shared.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.regex.Pattern;

@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class Email {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    @Column(name = "correo")
    private String value;

    private Email(String value) {
        if (value == null || !EMAIL_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid correo format: " + value);
        }
        this.value = value.toLowerCase().trim();
    }

    public static Email of(String value) {
        return new Email(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
