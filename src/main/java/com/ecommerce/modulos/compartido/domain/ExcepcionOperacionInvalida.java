package com.ecommerce.modulos.compartido.domain;

public class ExcepcionOperacionInvalida extends RuntimeException {

    public ExcepcionOperacionInvalida(String message) {
        super(message);
    }
}
