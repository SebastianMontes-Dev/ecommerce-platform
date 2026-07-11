package com.ecommerce.modules.identity.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid correo format")
    private String correo;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String contrasena;

    @Size(min = 8, message = "Password must be at least 8 characters")
    private String confirmPassword;

    @NotBlank(message = "First nombre is required")
    private String nombre;

    @NotBlank(message = "Last nombre is required")
    private String apellido;
}
