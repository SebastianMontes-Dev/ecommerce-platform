package com.ecommerce.modulos.resenas.application.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SolicitudCrearResenaValidationTest {

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
    void rechazaTituloConMasDe200Caracteres() {
        String tituloLargo = "a".repeat(201);
        SolicitudCrearResena solicitud = new SolicitudCrearResena();
        solicitud.setIdOrden(UUID.randomUUID());
        solicitud.setCalificacion(5);
        solicitud.setTitulo(tituloLargo);
        solicitud.setComentario("Buen producto");

        Set<ConstraintViolation<SolicitudCrearResena>> violations = validator.validate(solicitud);

        assertFalse(violations.isEmpty());
    }

    @Test
    void aceptaTituloConHasta200Caracteres() {
        String titulo = "a".repeat(200);
        SolicitudCrearResena solicitud = new SolicitudCrearResena();
        solicitud.setIdOrden(UUID.randomUUID());
        solicitud.setCalificacion(5);
        solicitud.setTitulo(titulo);
        solicitud.setComentario("Buen producto");

        assertTrue(validator.validate(solicitud).isEmpty());
    }

    @Test
    void rechazaComentarioConMasDe2000Caracteres() {
        String comentarioLargo = "a".repeat(2001);
        SolicitudCrearResena solicitud = new SolicitudCrearResena();
        solicitud.setIdOrden(UUID.randomUUID());
        solicitud.setCalificacion(5);
        solicitud.setTitulo("Excelente");
        solicitud.setComentario(comentarioLargo);

        Set<ConstraintViolation<SolicitudCrearResena>> violations = validator.validate(solicitud);

        assertFalse(violations.isEmpty());
    }

    @Test
    void aceptaComentarioConHasta2000Caracteres() {
        String comentario = "a".repeat(2000);
        SolicitudCrearResena solicitud = new SolicitudCrearResena();
        solicitud.setIdOrden(UUID.randomUUID());
        solicitud.setCalificacion(5);
        solicitud.setTitulo("Excelente");
        solicitud.setComentario(comentario);

        assertTrue(validator.validate(solicitud).isEmpty());
    }
}
