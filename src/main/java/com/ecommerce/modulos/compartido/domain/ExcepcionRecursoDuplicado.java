package com.ecommerce.modulos.compartido.domain;

public class ExcepcionRecursoDuplicado extends RuntimeException {

    public ExcepcionRecursoDuplicado(String resource, String field, String value) {
        super(String.format("%s with %s '%s' already exists", resource, field, value));
    }
}
