package com.ecommerce.modulos.catalogo.infrastructure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ValidadorArchivoImagenTest {

    @ParameterizedTest
    @CsvSource({
            "image/jpeg, jpg",
            "image/png, png",
            "image/webp, webp",
            "image/gif, gif"
    })
    void debeGenerarNombreSeguroConLaExtensionQueCorrespondeAlContentType(String contentType, String extensionEsperada) {
        UUID id = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "cualquier-nombre.bin", contentType, new byte[]{1, 2, 3});

        String nombreSeguro = ValidadorArchivoImagen.validarYGenerarNombreSeguro(file, id);

        assertEquals(id + "." + extensionEsperada, nombreSeguro);
    }

    @Test
    void debeIgnorarElNombreDeArchivoOriginalDelClienteAunSiIntentaPathTraversal() {
        UUID id = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "../../../etc/passwd.png", "image/png", new byte[]{1});

        String nombreSeguro = ValidadorArchivoImagen.validarYGenerarNombreSeguro(file, id);

        assertEquals(id + ".png", nombreSeguro);
    }

    @ParameterizedTest
    @ValueSource(strings = {"text/html", "image/svg+xml", "application/javascript", "application/x-msdownload"})
    void debeRechazarContentTypesNoPermitidos(String contentTypeNoPermitido) {
        MockMultipartFile file = new MockMultipartFile("file", "archivo", contentTypeNoPermitido, new byte[]{1});

        assertThrows(IllegalArgumentException.class,
                () -> ValidadorArchivoImagen.validarYGenerarNombreSeguro(file, UUID.randomUUID()));
    }

    @Test
    void debeRechazarArchivoSinContentType() {
        MockMultipartFile file = new MockMultipartFile("file", "archivo", null, new byte[]{1});

        assertThrows(IllegalArgumentException.class,
                () -> ValidadorArchivoImagen.validarYGenerarNombreSeguro(file, UUID.randomUUID()));
    }
}
