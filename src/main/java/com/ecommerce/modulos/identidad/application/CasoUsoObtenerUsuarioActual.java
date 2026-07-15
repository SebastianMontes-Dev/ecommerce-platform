package com.ecommerce.modulos.identidad.application;

import com.ecommerce.modulos.identidad.application.dto.RespuestaUsuario;
import com.ecommerce.modulos.identidad.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CasoUsoObtenerUsuarioActual {

    private final RepositorioUsuario repositorioUsuario;

    public RespuestaUsuario execute() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof DetallesUsuarioPersonalizado userDetails)) {
            return null;
        }

        return repositorioUsuario.findById(userDetails.getUserId())
                .map(this::mapToResponse)
                .orElse(null);
    }

    private RespuestaUsuario mapToResponse(Usuario usuario) {
        return RespuestaUsuario.builder()
                .id(usuario.getId())
                .correo(usuario.getCorreo())
                .nombre(usuario.getNombre())
                .apellido(usuario.getApellido())
                .nombreCompleto(usuario.getNombreCompleto())
                .emailVerified(usuario.isEmailVerified())
                .enabled(usuario.isEnabled())
                .roles(usuario.getRoles().stream().map(Enum::name).collect(Collectors.toSet()))
                .creadoEn(usuario.getCreadoEn())
                .build();
    }
}
