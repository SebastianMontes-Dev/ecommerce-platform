package com.ecommerce.modulos.identidad.application;

import com.ecommerce.modulos.identidad.application.dto.*;
import com.ecommerce.modulos.identidad.domain.*;
import com.ecommerce.modulos.compartido.domain.ExcepcionViolacionReglaNegocio;
import com.ecommerce.modulos.compartido.domain.ExcepcionRecursoDuplicado;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CasoUsoRegistrarUsuario {

    private final RepositorioUsuario repositorioUsuario;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public RespuestaUsuario execute(SolicitudRegistro request) {
        List<String> violations = new java.util.ArrayList<>();

        if (!request.getContrasena().equals(request.getConfirmarContrasena())) {
            violations.add("Las contraseñas no coinciden");
        }

        if (repositorioUsuario.existsByCorreo(request.getCorreo())) {
            violations.add("El correo ya está registrado");
        }

        if (!violations.isEmpty()) {
            throw new ExcepcionViolacionReglaNegocio(violations);
        }

        Usuario usuario = new Usuario(
                request.getCorreo().toLowerCase().trim(),
                passwordEncoder.encode(request.getContrasena()),
                request.getNombre(),
                request.getApellido()
        );
        usuario.addRole(RolUsuario.CUSTOMER);

        usuario = repositorioUsuario.save(usuario);

        return mapToResponse(usuario);
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
