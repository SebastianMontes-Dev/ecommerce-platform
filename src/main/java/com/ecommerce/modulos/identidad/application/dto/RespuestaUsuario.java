package com.ecommerce.modulos.identidad.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RespuestaUsuario {

    private UUID id;
    private String correo;
    private String nombre;
    private String apellido;
    private String nombreCompleto;
    private boolean emailVerified;
    private boolean enabled;
    private Set<String> roles;
    private LocalDateTime creadoEn;
}
