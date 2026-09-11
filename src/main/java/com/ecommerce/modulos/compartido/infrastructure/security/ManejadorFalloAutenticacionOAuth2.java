package com.ecommerce.modulos.compartido.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * Simétrico a {@link ManejadorExitoAutenticacionOAuth2}: sin un failure handler propio, un
 * rechazo del proveedor (usuario cancela, el correo no viene en el perfil, etc.) caía en la
 * página blanca de error por defecto de Spring Security en vez de volver al frontend.
 */
@Component
@Slf4j
public class ManejadorFalloAutenticacionOAuth2 implements AuthenticationFailureHandler {

    private final String frontendRedirectUri;

    public ManejadorFalloAutenticacionOAuth2(@Value("${app.oauth2.frontend-redirect-uri}") String frontendRedirectUri) {
        this.frontendRedirectUri = frontendRedirectUri;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException {
        log.warn("Login OAuth2 fallido: {}", exception.getMessage());
        String destino = UriComponentsBuilder.fromUriString(frontendRedirectUri)
                .queryParam("error", "oauth2_login_failed")
                .build()
                .toUriString();
        response.sendRedirect(destino);
    }
}
