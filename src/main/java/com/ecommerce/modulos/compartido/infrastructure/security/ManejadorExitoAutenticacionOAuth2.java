package com.ecommerce.modulos.compartido.infrastructure.security;

import com.ecommerce.modulos.identidad.application.DetallesUsuarioPersonalizado;
import com.ecommerce.modulos.identidad.domain.RepositorioTokenActualizacion;
import com.ecommerce.modulos.identidad.domain.RepositorioUsuario;
import com.ecommerce.modulos.identidad.domain.TokenActualizacion;
import com.ecommerce.modulos.identidad.domain.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Sin este handler, un login social exitoso terminaba en un callejón sin salida:
 * {@link CustomOAuth2UserService} crea o encuentra al {@link Usuario}, pero Spring Security solo
 * deja un {@code OAuth2AuthenticationToken} en un {@code SecurityContext} que, al ser la app
 * {@code STATELESS} (sin sesión), se descarta al terminar la request — el resto de la API, que
 * solo entiende el JWT propio, nunca se enteraba de que el login pasó.
 *
 * <p>Emite el mismo par access/refresh token que {@code CasoUsoIniciarSesion} (login por
 * contraseña) y redirige al frontend llevándolo en el <b>fragmento</b> de la URL
 * ({@code #access_token=...}), no en el query string: el fragmento nunca viaja al servidor en
 * la navegación del browser, así que no queda en logs de acceso ni se reenvía a terceros.
 */
@Component
@Slf4j
public class ManejadorExitoAutenticacionOAuth2 implements AuthenticationSuccessHandler {

    private final RepositorioUsuario repositorioUsuario;
    private final RepositorioTokenActualizacion repositorioTokenActualizacion;
    private final ProveedorTokenJwt proveedorTokenJwt;
    private final String frontendRedirectUri;

    public ManejadorExitoAutenticacionOAuth2(
            RepositorioUsuario repositorioUsuario,
            RepositorioTokenActualizacion repositorioTokenActualizacion,
            ProveedorTokenJwt proveedorTokenJwt,
            @Value("${app.oauth2.frontend-redirect-uri}") String frontendRedirectUri) {
        this.repositorioUsuario = repositorioUsuario;
        this.repositorioTokenActualizacion = repositorioTokenActualizacion;
        this.proveedorTokenJwt = proveedorTokenJwt;
        this.frontendRedirectUri = frontendRedirectUri;
    }

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {
        String correo = ((OAuth2User) authentication.getPrincipal()).getAttribute("email");
        Usuario usuario = repositorioUsuario.findByCorreo(correo)
                .orElseThrow(() -> new IllegalStateException(
                        "CustomOAuth2UserService debía haber creado el usuario " + correo + " antes de este handler"));

        DetallesUsuarioPersonalizado userDetails = new DetallesUsuarioPersonalizado(usuario);
        String accessToken = proveedorTokenJwt.generateAccessToken(userDetails);
        String refreshToken = proveedorTokenJwt.generateRefreshToken();

        repositorioTokenActualizacion.save(new TokenActualizacion(
                refreshToken,
                usuario.getId(),
                LocalDateTime.now().plusSeconds(proveedorTokenJwt.getRefreshTokenExpiration() / 1000)));

        String destino = UriComponentsBuilder.fromUriString(frontendRedirectUri)
                .fragment("access_token=" + accessToken
                        + "&refresh_token=" + refreshToken
                        + "&expires_in=" + (proveedorTokenJwt.getAccessTokenExpiration() / 1000))
                .build()
                .toUriString();

        log.info("Login OAuth2 exitoso para {}", correo);
        response.sendRedirect(destino);
    }
}
