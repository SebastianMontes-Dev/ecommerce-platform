package com.ecommerce.modulos.identidad.infrastructure;

import com.ecommerce.modulos.identidad.application.*;
import com.ecommerce.modulos.identidad.application.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Usuario registration, login, and token management")
public class ControladorAutenticacion {

    private final CasoUsoRegistrarUsuario casoUsoRegistrarUsuario;
    private final CasoUsoIniciarSesion casoUsoIniciarSesion;
    private final CasoUsoActualizarToken casoUsoActualizarToken;
    private final CasoUsoObtenerUsuarioActual casoUsoObtenerUsuarioActual;

    @PostMapping("/registro")
    @Operation(summary = "Register a new usuario")
    public ResponseEntity<RespuestaUsuario> register(@Valid @RequestBody SolicitudRegistro request) {
        RespuestaUsuario response = casoUsoRegistrarUsuario.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Login and obtain JWT tokens")
    public ResponseEntity<RespuestaAutenticacion> login(@Valid @RequestBody SolicitudInicioSesion request) {
        RespuestaAutenticacion response = casoUsoIniciarSesion.execute(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/actualizar")
    @Operation(summary = "Refresh access token using refresh token")
    public ResponseEntity<RespuestaAutenticacion> refresh(@Valid @RequestBody SolicitudActualizarToken request) {
        RespuestaAutenticacion response = casoUsoActualizarToken.execute(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/yo")
    @Operation(summary = "Get current authenticated usuario profile")
    public ResponseEntity<RespuestaUsuario> me() {
        RespuestaUsuario response = casoUsoObtenerUsuarioActual.execute();
        if (response == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(response);
    }
}
