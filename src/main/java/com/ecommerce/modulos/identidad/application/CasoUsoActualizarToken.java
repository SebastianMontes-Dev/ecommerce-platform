package com.ecommerce.modulos.identidad.application;

import com.ecommerce.modulos.identidad.application.dto.RespuestaAutenticacion;
import com.ecommerce.modulos.identidad.application.dto.SolicitudActualizarToken;
import com.ecommerce.modulos.identidad.domain.*;
import com.ecommerce.modulos.compartido.domain.ExcepcionNoAutorizado;
import com.ecommerce.modulos.compartido.infrastructure.security.ProveedorTokenJwt;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CasoUsoActualizarToken {

    private final RepositorioTokenActualizacion repositorioTokenActualizacion;
    private final ProveedorTokenJwt proveedorTokenJwt;
    private final RepositorioUsuario repositorioUsuario;

    @Transactional
    public RespuestaAutenticacion execute(SolicitudActualizarToken request) {
        TokenActualizacion storedToken = repositorioTokenActualizacion.findByToken(request.getTokenActualizacion())
                .orElseThrow(() -> new ExcepcionNoAutorizado("Invalid refresh token"));

        if (!storedToken.isValid()) {
            throw ExcepcionNoAutorizado.tokenExpired();
        }

        storedToken.revoke();
        repositorioTokenActualizacion.save(storedToken);

        Usuario usuario = repositorioUsuario.findById(storedToken.getUserId())
                .orElseThrow(() -> new ExcepcionNoAutorizado("Usuario not found"));

        DetallesUsuarioPersonalizado userDetails = new DetallesUsuarioPersonalizado(usuario);

        String newAccessToken = proveedorTokenJwt.generateAccessToken(userDetails);
        String newRefreshTokenValue = proveedorTokenJwt.generateRefreshToken();
        long accessExpiration = proveedorTokenJwt.getAccessTokenExpiration();

        TokenActualizacion newRefreshToken = new TokenActualizacion(
                newRefreshTokenValue,
                usuario.getId(),
                LocalDateTime.now().plusSeconds(proveedorTokenJwt.getRefreshTokenExpiration() / 1000)
        );
        repositorioTokenActualizacion.save(newRefreshToken);

        return RespuestaAutenticacion.builder()
                .accessToken(newAccessToken)
                .tokenActualizacion(newRefreshTokenValue)
                .expiresIn(accessExpiration / 1000)
                .tokenType("Bearer")
                .build();
    }
}
