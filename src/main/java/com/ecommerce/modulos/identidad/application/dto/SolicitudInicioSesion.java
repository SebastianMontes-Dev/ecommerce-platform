package com.ecommerce.modulos.identidad.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudInicioSesion {

    @NotBlank(message = "Correo is required")
    @Email(message = "Invalid correo format")
    private String correo;

    @NotBlank(message = "Password is required")
    private String contrasena;
}
