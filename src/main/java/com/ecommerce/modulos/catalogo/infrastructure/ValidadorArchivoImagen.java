package com.ecommerce.modulos.catalogo.infrastructure;

import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Map;

/**
 * Sanitiza archivos subidos por el vendedor antes de guardarlos en MinIO. El
 * nombre y el content-type que llegan en el MultipartFile los controla el
 * cliente, asi que no se usan tal cual: el content-type se valida contra un
 * allowlist de imagenes, y el nombre original se descarta por completo para
 * el object key (evita path traversal via "../" y la extension real la decide
 * el content-type validado, no lo que diga el nombre de archivo del cliente).
 */
public final class ValidadorArchivoImagen {

    private static final Map<String, String> EXTENSION_POR_CONTENT_TYPE = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp",
            "image/gif", "gif"
    );

    private ValidadorArchivoImagen() {
    }

    /**
     * Valida que el archivo sea una imagen de un tipo permitido y devuelve un
     * nombre de object key seguro (UUID aleatorio + extension derivada del
     * content-type real, nunca del nombre de archivo del cliente).
     */
    public static String validarYGenerarNombreSeguro(MultipartFile file, java.util.UUID id) {
        String contentType = file.getContentType();
        String extension = contentType != null
                ? EXTENSION_POR_CONTENT_TYPE.get(contentType.toLowerCase(Locale.ROOT))
                : null;

        if (extension == null) {
            throw new IllegalArgumentException(
                    "Tipo de archivo no permitido: solo se aceptan imagenes JPEG, PNG, WEBP o GIF");
        }

        return id + "." + extension;
    }
}
