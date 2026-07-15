package com.ecommerce.modulos.compartido.domain;

public class ExcepcionNoAutorizado extends RuntimeException {

    public ExcepcionNoAutorizado(String message) {
        super(message);
    }

    public static ExcepcionNoAutorizado invalidCredentials() {
        return new ExcepcionNoAutorizado("Invalid correo or contrasena");
    }

    public static ExcepcionNoAutorizado tokenExpired() {
        return new ExcepcionNoAutorizado("Token has expired");
    }

    public static ExcepcionNoAutorizado insufficientPermissions() {
        return new ExcepcionNoAutorizado("Insufficient permissions to perform this action");
    }
}
