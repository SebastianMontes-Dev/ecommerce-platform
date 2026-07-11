package com.ecommerce.modules.shared.domain;

public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }

    public static UnauthorizedException invalidCredentials() {
        return new UnauthorizedException("Invalid correo or contrasena");
    }

    public static UnauthorizedException tokenExpired() {
        return new UnauthorizedException("Token has expired");
    }

    public static UnauthorizedException insufficientPermissions() {
        return new UnauthorizedException("Insufficient permissions to perform this action");
    }
}
