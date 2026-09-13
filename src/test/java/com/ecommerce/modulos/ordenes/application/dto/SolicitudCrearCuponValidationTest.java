package com.ecommerce.modulos.ordenes.application.dto;

import com.ecommerce.modulos.ordenes.domain.TipoDescuento;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SolicitudCrearCuponValidationTest {

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
    void rechazaPorcentajeMayorA100() {
        SolicitudCrearCupon solicitud = SolicitudCrearCupon.builder()
                .codigo("PROMO")
                .tipo(TipoDescuento.PORCENTAJE)
                .valor(new BigDecimal("500"))
                .build();

        Set<ConstraintViolation<SolicitudCrearCupon>> violations = validator.validate(solicitud);

        assertFalse(violations.isEmpty());
    }

    @Test
    void aceptaPorcentajeDentroDeRango() {
        SolicitudCrearCupon solicitud = SolicitudCrearCupon.builder()
                .codigo("PROMO")
                .tipo(TipoDescuento.PORCENTAJE)
                .valor(new BigDecimal("50"))
                .build();

        assertTrue(validator.validate(solicitud).isEmpty());
    }

    @Test
    void aceptaMontoFijoMayorA100() {
        SolicitudCrearCupon solicitud = SolicitudCrearCupon.builder()
                .codigo("PROMO")
                .tipo(TipoDescuento.MONTO_FIJO)
                .valor(new BigDecimal("500"))
                .build();

        assertTrue(validator.validate(solicitud).isEmpty());
    }
}
