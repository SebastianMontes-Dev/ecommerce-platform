package com.ecommerce.modulos.compartido.infrastructure.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProveedorTokenJwtTest {

    private static final String SECRET = "una-clave-secreta-de-prueba-con-al-menos-32-bytes-de-longitud";

    private PropiedadesJwt propiedadesJwt;
    private ProveedorTokenJwt proveedorTokenJwt;

    @BeforeEach
    void setUp() {
        propiedadesJwt = new PropiedadesJwt();
        propiedadesJwt.setSecret(SECRET);
        propiedadesJwt.setAccessTokenExpiration(60_000L);
        propiedadesJwt.setRefreshTokenExpiration(3_600_000L);
        proveedorTokenJwt = new ProveedorTokenJwt(propiedadesJwt);
    }

    private UserDetails usuarioDePrueba() {
        return User.withUsername("cliente@test.com")
                .password("irrelevante")
                .authorities(List.of())
                .build();
    }

    @Test
    void generateAccessTokenCreaUnTokenDelQuePuedeExtraerseElUsername() {
        String token = proveedorTokenJwt.generateAccessToken(usuarioDePrueba());

        assertNotNull(token);
        assertEquals("cliente@test.com", proveedorTokenJwt.getUsernameFromToken(token));
    }

    @Test
    void generateAccessTokenLanzaExcepcionSiUserDetailsEsNulo() {
        assertThrows(IllegalArgumentException.class, () -> proveedorTokenJwt.generateAccessToken(null));
    }

    @Test
    void validateTokenDevuelveTrueParaUnTokenValidoYNoExpirado() {
        String token = proveedorTokenJwt.generateAccessToken(usuarioDePrueba());

        assertTrue(proveedorTokenJwt.validateToken(token));
    }

    @Test
    void validateTokenDevuelveFalseParaUnTokenExpirado() {
        String tokenExpirado = construirTokenConExpiracion(SECRET, new Date(System.currentTimeMillis() - 10_000));

        assertFalse(proveedorTokenJwt.validateToken(tokenExpirado));
    }

    @Test
    void validateTokenDevuelveFalseParaUnTokenFirmadoConOtraClave() {
        String otraClave = "otra-clave-secreta-completamente-distinta-32-bytes";
        String tokenConFirmaAjena = construirTokenConExpiracion(otraClave, new Date(System.currentTimeMillis() + 60_000));

        assertFalse(proveedorTokenJwt.validateToken(tokenConFirmaAjena));
    }

    @Test
    void validateTokenDevuelveFalseParaUnTokenManipulado() {
        String token = proveedorTokenJwt.generateAccessToken(usuarioDePrueba());
        String tokenManipulado = token.substring(0, token.length() - 2) + "xx";

        assertFalse(proveedorTokenJwt.validateToken(tokenManipulado));
    }

    @Test
    void validateTokenDevuelveFalseParaUnaCadenaMalformada() {
        assertFalse(proveedorTokenJwt.validateToken("esto-no-es-un-jwt"));
    }

    @Test
    void getUsernameFromTokenLanzaExcepcionSiElTokenEsInvalido() {
        assertThrows(Exception.class, () -> proveedorTokenJwt.getUsernameFromToken("token-invalido"));
    }

    @Test
    void generateRefreshTokenDevuelveValoresUnicosEnCadaLlamada() {
        String primero = proveedorTokenJwt.generateRefreshToken();
        String segundo = proveedorTokenJwt.generateRefreshToken();

        assertNotEquals(primero, segundo);
        assertTrue(primero.contains("-"));
    }

    @Test
    void getAccessTokenExpirationDelegaEnPropiedadesJwt() {
        assertEquals(60_000L, proveedorTokenJwt.getAccessTokenExpiration());
    }

    @Test
    void getRefreshTokenExpirationDelegaEnPropiedadesJwt() {
        assertEquals(3_600_000L, proveedorTokenJwt.getRefreshTokenExpiration());
    }

    private String construirTokenConExpiracion(String secret, Date expiracion) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject("cliente@test.com")
                .issuedAt(new Date(System.currentTimeMillis() - 60_000))
                .expiration(expiracion)
                .signWith(key)
                .compact();
    }
}
