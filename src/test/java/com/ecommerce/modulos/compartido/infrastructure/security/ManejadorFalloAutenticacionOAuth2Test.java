package com.ecommerce.modulos.compartido.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

import static org.junit.jupiter.api.Assertions.*;

class ManejadorFalloAutenticacionOAuth2Test {

    private final ManejadorFalloAutenticacionOAuth2 manejador =
            new ManejadorFalloAutenticacionOAuth2("http://localhost:3000/oauth2/callback");

    @Test
    void redirigeAlFrontendConUnParametroDeError() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        manejador.onAuthenticationFailure(new MockHttpServletRequest(), response,
                new OAuth2AuthenticationException(new OAuth2Error("access_denied"), "El usuario canceló el login"));

        assertEquals(302, response.getStatus());
        assertEquals("http://localhost:3000/oauth2/callback?error=oauth2_login_failed", response.getRedirectedUrl());
    }
}
