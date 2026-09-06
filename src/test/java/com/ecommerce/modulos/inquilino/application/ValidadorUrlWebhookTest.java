package com.ecommerce.modulos.inquilino.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidadorUrlWebhookTest {

    private final ValidadorUrlWebhook validador = new ValidadorUrlWebhook();

    @ParameterizedTest
    @ValueSource(strings = {
            "http://93.184.216.34/webhook",
            "https://93.184.216.34:8443/webhook",
            "http://8.8.8.8/webhook"
    })
    void debeAceptarUrlsHttpOHttpsConIpPublicaLiteral(String url) {
        assertTrue(validador.esSegura(url));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://127.0.0.1/webhook",
            "http://localhost/webhook",
            "http://169.254.169.254/latest/meta-data/",
            "http://10.0.0.5/webhook",
            "http://172.16.0.1/webhook",
            "http://192.168.1.1/webhook",
            "http://0.0.0.0/webhook",
            "http://[::1]/webhook"
    })
    void debeRechazarUrlsQueApuntanAInfraestructuraNoPublica(String url) {
        assertFalse(validador.esSegura(url));
    }

    @Test
    void debeRechazarEsquemasDistintosDeHttpYHttps() {
        assertFalse(validador.esSegura("ftp://93.184.216.34/webhook"));
        assertFalse(validador.esSegura("file:///etc/passwd"));
        assertFalse(validador.esSegura("gopher://93.184.216.34/webhook"));
    }

    @Test
    void debeRechazarUrlsMalformadas() {
        assertFalse(validador.esSegura("no-es-una-url"));
        assertFalse(validador.esSegura(""));
    }
}
