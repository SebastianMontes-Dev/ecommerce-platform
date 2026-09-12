package com.ecommerce.modulos.catalogo.application.dto;

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

class SolicitudCrearProductoValidationTest {

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
    void rechazaNombreConMasDe200Caracteres() {
        String nombreLargo = "a".repeat(201);
        SolicitudCrearProducto solicitud = SolicitudCrearProducto.builder()
                .nombre(nombreLargo)
                .enlaceCorto("link")
                .precio(new BigDecimal("100.00"))
                .inventario(1)
                .build();

        Set<ConstraintViolation<SolicitudCrearProducto>> violations = validator.validate(solicitud);

        assertFalse(violations.isEmpty());
    }

    @Test
    void aceptaNombreConHasta200Caracteres() {
        String nombre = "a".repeat(200);
        SolicitudCrearProducto solicitud = SolicitudCrearProducto.builder()
                .nombre(nombre)
                .enlaceCorto("link")
                .precio(new BigDecimal("100.00"))
                .inventario(1)
                .build();

        assertTrue(validator.validate(solicitud).isEmpty());
    }

    @Test
    void rechazaEnlaceCortoConMasDe200Caracteres() {
        String enlaceCortoLargo = "a".repeat(201);
        SolicitudCrearProducto solicitud = SolicitudCrearProducto.builder()
                .nombre("Producto")
                .enlaceCorto(enlaceCortoLargo)
                .precio(new BigDecimal("100.00"))
                .inventario(1)
                .build();

        Set<ConstraintViolation<SolicitudCrearProducto>> violations = validator.validate(solicitud);

        assertFalse(violations.isEmpty());
    }

    @Test
    void aceptaEnlaceCortoConHasta200Caracteres() {
        String enlaceCorto = "a".repeat(200);
        SolicitudCrearProducto solicitud = SolicitudCrearProducto.builder()
                .nombre("Producto")
                .enlaceCorto(enlaceCorto)
                .precio(new BigDecimal("100.00"))
                .inventario(1)
                .build();

        assertTrue(validator.validate(solicitud).isEmpty());
    }

    @Test
    void rechazaDescripcionConMasDe5000Caracteres() {
        String descripcionLarga = "a".repeat(5001);
        SolicitudCrearProducto solicitud = SolicitudCrearProducto.builder()
                .nombre("Producto")
                .enlaceCorto("link")
                .descripcion(descripcionLarga)
                .precio(new BigDecimal("100.00"))
                .inventario(1)
                .build();

        Set<ConstraintViolation<SolicitudCrearProducto>> violations = validator.validate(solicitud);

        assertFalse(violations.isEmpty());
    }

    @Test
    void aceptaDescripcionConHasta5000Caracteres() {
        String descripcion = "a".repeat(5000);
        SolicitudCrearProducto solicitud = SolicitudCrearProducto.builder()
                .nombre("Producto")
                .enlaceCorto("link")
                .descripcion(descripcion)
                .precio(new BigDecimal("100.00"))
                .inventario(1)
                .build();

        assertTrue(validator.validate(solicitud).isEmpty());
    }

    @Test
    void rechazaSkuConMasDe100Caracteres() {
        String skuLargo = "a".repeat(101);
        SolicitudCrearProducto solicitud = SolicitudCrearProducto.builder()
                .nombre("Producto")
                .enlaceCorto("link")
                .sku(skuLargo)
                .precio(new BigDecimal("100.00"))
                .inventario(1)
                .build();

        Set<ConstraintViolation<SolicitudCrearProducto>> violations = validator.validate(solicitud);

        assertFalse(violations.isEmpty());
    }

    @Test
    void aceptaSkuConHasta100Caracteres() {
        String sku = "a".repeat(100);
        SolicitudCrearProducto solicitud = SolicitudCrearProducto.builder()
                .nombre("Producto")
                .enlaceCorto("link")
                .sku(sku)
                .precio(new BigDecimal("100.00"))
                .inventario(1)
                .build();

        assertTrue(validator.validate(solicitud).isEmpty());
    }

    @Test
    void rechazaCodigoBarrasConMasDe100Caracteres() {
        String codigoBarrasLargo = "a".repeat(101);
        SolicitudCrearProducto solicitud = SolicitudCrearProducto.builder()
                .nombre("Producto")
                .enlaceCorto("link")
                .codigoBarras(codigoBarrasLargo)
                .precio(new BigDecimal("100.00"))
                .inventario(1)
                .build();

        Set<ConstraintViolation<SolicitudCrearProducto>> violations = validator.validate(solicitud);

        assertFalse(violations.isEmpty());
    }

    @Test
    void aceptaCodigoBarrasConHasta100Caracteres() {
        String codigoBarras = "a".repeat(100);
        SolicitudCrearProducto solicitud = SolicitudCrearProducto.builder()
                .nombre("Producto")
                .enlaceCorto("link")
                .codigoBarras(codigoBarras)
                .precio(new BigDecimal("100.00"))
                .inventario(1)
                .build();

        assertTrue(validator.validate(solicitud).isEmpty());
    }

    @Test
    void aceptaInventarioEnCeroParaUnProductoAgotadoODigital() {
        SolicitudCrearProducto solicitud = SolicitudCrearProducto.builder()
                .nombre("Producto")
                .enlaceCorto("link")
                .precio(new BigDecimal("100.00"))
                .inventario(0)
                .build();

        assertTrue(validator.validate(solicitud).isEmpty());
    }
}
