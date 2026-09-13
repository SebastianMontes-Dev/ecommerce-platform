package com.ecommerce.modulos.identidad.application.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SolicitudRegistroValidationTest {

    static ValidatorFactory factory;
    static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    void rechazaContrasenaConMenosDe8Caracteres() {
        SolicitudRegistro solicitud = SolicitudRegistro.builder()
                .correo("test@example.com")
                .contrasena("short")
                .confirmarContrasena("short")
                .nombre("Juan")
                .apellido("Pérez")
                .build();

        Set<ConstraintViolation<SolicitudRegistro>> violations = validator.validate(solicitud);

        assertFalse(violations.isEmpty());
    }

    @Test
    void rechazaContrasenaConMasDe100Caracteres() {
        String contrasenaLarga = "a".repeat(101);
        SolicitudRegistro solicitud = SolicitudRegistro.builder()
                .correo("test@example.com")
                .contrasena(contrasenaLarga)
                .confirmarContrasena(contrasenaLarga)
                .nombre("Juan")
                .apellido("Pérez")
                .build();

        Set<ConstraintViolation<SolicitudRegistro>> violations = validator.validate(solicitud);

        assertFalse(violations.isEmpty());
    }

    @Test
    void aceptaContrasenaValida() {
        SolicitudRegistro solicitud = SolicitudRegistro.builder()
                .correo("test@example.com")
                .contrasena("ValidPassword123")
                .confirmarContrasena("ValidPassword123")
                .nombre("Juan")
                .apellido("Pérez")
                .build();

        assertTrue(validator.validate(solicitud).isEmpty());
    }

    @Test
    void aceptaContrasenaDe100Caracteres() {
        String contrasena = "a".repeat(100);
        SolicitudRegistro solicitud = SolicitudRegistro.builder()
                .correo("test@example.com")
                .contrasena(contrasena)
                .confirmarContrasena(contrasena)
                .nombre("Juan")
                .apellido("Pérez")
                .build();

        assertTrue(validator.validate(solicitud).isEmpty());
    }

    @Test
    void aceptaContrasenaDe8Caracteres() {
        SolicitudRegistro solicitud = SolicitudRegistro.builder()
                .correo("test@example.com")
                .contrasena("12345678")
                .confirmarContrasena("12345678")
                .nombre("Juan")
                .apellido("Pérez")
                .build();

        assertTrue(validator.validate(solicitud).isEmpty());
    }
}
