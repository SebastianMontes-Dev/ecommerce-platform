package com.ecommerce.modulos.inquilino.domain;

import com.ecommerce.modulos.compartido.domain.Imagen;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InquilinoTest {

    private UUID idPropietario;
    private Inquilino inquilino;

    @BeforeEach
    void setUp() {
        idPropietario = UUID.randomUUID();
        inquilino = new Inquilino("Mi Tienda", "mi-tienda", idPropietario);
    }

    @Test
    void debeIniciarEnEstadoTrialAlCrearse() {
        assertEquals(EstadoInquilino.TRIAL, inquilino.getEstado());
    }

    @Test
    void debeLanzarExcepcionAlCrearseConEnlaceCortoInvalido() {
        assertThrows(IllegalArgumentException.class,
                () -> new Inquilino("Mi Tienda", "Enlace Invalido!", idPropietario));
    }

    @Test
    void debeActivarseCuandoEstaEnTrial() {
        // Act
        inquilino.activate();

        // Assert
        assertEquals(EstadoInquilino.ACTIVE, inquilino.getEstado());
    }

    @Test
    void debeSuspenderseCuandoEstaEnTrial() {
        // Act
        inquilino.suspend();

        // Assert
        assertEquals(EstadoInquilino.SUSPENDED, inquilino.getEstado());
    }

    @Test
    void debeSuspenderseCuandoEstaActivo() {
        // Arrange
        inquilino.activate();

        // Act
        inquilino.suspend();

        // Assert
        assertEquals(EstadoInquilino.SUSPENDED, inquilino.getEstado());
    }

    @Test
    void debeCancelarseDesdeCualquierEstadoNoCancelado() {
        // Act
        inquilino.cancel();

        // Assert
        assertEquals(EstadoInquilino.CANCELLED, inquilino.getEstado());
    }

    @Test
    void debeLanzarExcepcionAlSuspenderUnInquilinoCancelado() {
        // Arrange
        inquilino.cancel();

        // Act & Assert
        IllegalStateException excepcion = assertThrows(IllegalStateException.class, inquilino::suspend);
        assertEquals("Cannot suspend a cancelled inquilino", excepcion.getMessage());
    }

    @Test
    void debeLanzarExcepcionAlActivarUnInquilinoCancelado() {
        // Arrange
        inquilino.cancel();

        // Act & Assert
        IllegalStateException excepcion = assertThrows(IllegalStateException.class, inquilino::activate);
        assertEquals("Cannot activate a cancelled inquilino", excepcion.getMessage());
    }

    @Test
    void isActiveDebeSerVerdaderoCuandoElEstadoEsTrial() {
        assertTrue(inquilino.isActive());
    }

    @Test
    void isActiveDebeSerVerdaderoCuandoElEstadoEsActive() {
        inquilino.activate();
        assertTrue(inquilino.isActive());
    }

    @Test
    void isActiveDebeSerFalsoCuandoElEstadoEsSuspended() {
        inquilino.suspend();
        assertFalse(inquilino.isActive());
    }

    @Test
    void isActiveDebeSerFalsoCuandoElEstadoEsCancelled() {
        inquilino.cancel();
        assertFalse(inquilino.isActive());
    }

    @Test
    void isOwnedByDebeSerVerdaderoCuandoElIdCoincideConElPropietario() {
        assertTrue(inquilino.isOwnedBy(idPropietario));
    }

    @Test
    void isOwnedByDebeSerFalsoCuandoElIdNoCoincideConElPropietario() {
        assertFalse(inquilino.isOwnedBy(UUID.randomUUID()));
    }

    @Test
    void getLogoDebeSerNuloCuandoNoSeHaConfiguradoUnaUrl() {
        assertNull(inquilino.getLogo());
    }

    @Test
    void getLogoDebeUsarTextoAlternativoPorDefectoCuandoNoSeConfiguroUno() {
        // Arrange
        inquilino.setUrlLogo("https://cdn.test/logo.png");

        // Act
        Imagen logo = inquilino.getLogo();

        // Assert
        assertEquals("https://cdn.test/logo.png", logo.getUrl());
        assertEquals("Mi Tienda logo", logo.getAltText());
    }

    @Test
    void getLogoDebeUsarElTextoAlternativoConfiguradoCuandoExiste() {
        // Arrange
        inquilino.setUrlLogo("https://cdn.test/logo.png");
        inquilino.setTextoAlternativoLogo("Logo personalizado");

        // Act
        Imagen logo = inquilino.getLogo();

        // Assert
        assertEquals("Logo personalizado", logo.getAltText());
    }

    @Test
    void getBannerDebeSerNuloCuandoNoSeHaConfiguradoUnaUrl() {
        assertNull(inquilino.getBanner());
    }

    @Test
    void getBannerDebeUsarTextoAlternativoPorDefectoCuandoNoSeConfiguroUno() {
        // Arrange
        inquilino.setUrlBanner("https://cdn.test/banner.png");

        // Act
        Imagen banner = inquilino.getBanner();

        // Assert
        assertEquals("https://cdn.test/banner.png", banner.getUrl());
        assertEquals("Mi Tienda banner", banner.getAltText());
    }
}
