package com.ecommerce.modules.shared.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class Address {

    @Column(name = "street")
    private String street;

    @Column(name = "city")
    private String city;

    @Column(name = "state")
    private String state;

    @Column(name = "zip_code")
    private String zipCode;

    @Column(name = "country")
    private String country;

    @Column(name = "additional_info")
    private String additionalInfo;

    private Address(String street, String city, String state, String zipCode, String country, String additionalInfo) {
        this.street = street;
        this.city = city;
        this.state = state;
        this.zipCode = zipCode;
        this.country = country;
        this.additionalInfo = additionalInfo;
    }

    public static Address of(String street, String city, String state, String zipCode, String country) {
        if (street == null || street.isBlank()) throw new IllegalArgumentException("Street is required");
        if (city == null || city.isBlank()) throw new IllegalArgumentException("City is required");
        if (country == null || country.isBlank()) throw new IllegalArgumentException("Country is required");
        return new Address(street, city, state, zipCode, country, null);
    }

    public static Address of(String street, String city, String state, String zipCode, String country, String additionalInfo) {
        if (street == null || street.isBlank()) throw new IllegalArgumentException("Street is required");
        if (city == null || city.isBlank()) throw new IllegalArgumentException("City is required");
        if (country == null || country.isBlank()) throw new IllegalArgumentException("Country is required");
        return new Address(street, city, state, zipCode, country, additionalInfo);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(street).append(", ").append(city);
        if (state != null) sb.append(", ").append(state);
        if (zipCode != null) sb.append(" ").append(zipCode);
        sb.append(", ").append(country);
        return sb.toString();
    }
}
