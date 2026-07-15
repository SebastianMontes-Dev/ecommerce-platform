package com.ecommerce.modulos.identidad.application;

import com.ecommerce.modulos.identidad.application.dto.*;
import com.ecommerce.modulos.identidad.domain.*;
import com.ecommerce.modulos.compartido.domain.ExcepcionNoAutorizado;
import com.ecommerce.modulos.compartido.infrastructure.security.ProveedorTokenJwt;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CasoUsoIniciarSesion {

    private final AuthenticationManager authenticationManager;
    private final ProveedorTokenJwt proveedorTokenJwt;
    private final RepositorioTokenActualizacion repositorioTokenActualizacion;
    private final RepositorioUsuario repositorioUsuario;

    @Transactional
    public RespuestaAutenticacion execute(SolicitudInicioSesion request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getCorreo().toLowerCase().trim(),
                            request.getContrasena()
                    )
            );

            DetallesUsuarioPersonalizado userDetails = (DetallesUsuarioPersonalizado) authentication.getPrincipal();
            Usuario usuario = repositorioUsuario.findByCorreo(userDetails.getUsername())
                    .orElseThrow(() -> new UsernameNotFoundException("Usuario not found"));

            if (!usuario.isEnabled()) {
                throw new ExcepcionNoAutorizado("Account is disabled");
            }

            String accessToken = proveedorTokenJwt.generateAccessToken(userDetails);
            String refreshTokenValue = proveedorTokenJwt.generateRefreshToken();
            long accessExpiration = proveedorTokenJwt.getAccessTokenExpiration();

            TokenActualizacion tokenActualizacion = new TokenActualizacion(
                    refreshTokenValue,
                    usuario.getId(),
                    LocalDateTime.now().plusSeconds(proveedorTokenJwt.getRefreshTokenExpiration() / 1000)
            );
            repositorioTokenActualizacion.save(tokenActualizacion);

            return RespuestaAutenticacion.builder()
                    .accessToken(accessToken)
                    .tokenActualizacion(refreshTokenValue)
                    .expiresIn(accessExpiration / 1000)
                    .tokenType("Bearer")
                    .build();
        } catch (BadCredentialsException e) {
            throw new ExcepcionNoAutorizado("Invalid correo or contrasena");
        }
    }
}
