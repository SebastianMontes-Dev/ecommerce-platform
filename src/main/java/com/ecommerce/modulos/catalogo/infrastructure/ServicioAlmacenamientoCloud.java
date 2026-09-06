package com.ecommerce.modulos.catalogo.infrastructure;

import io.minio.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ServicioAlmacenamientoCloud {

    private static final Logger log = LoggerFactory.getLogger(ServicioAlmacenamientoCloud.class);
    private final MinioClient minioClient;
    private final MinioConfig minioConfig;
    private static final String BUCKET = "ecommerce-images";

    public String uploadImage(MultipartFile file) {
        try {
            String objectName = ValidadorArchivoImagen.validarYGenerarNombreSeguro(file, UUID.randomUUID());
            InputStream inputStream = file.getInputStream();

            boolean bucketExists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(BUCKET).build());
            if (!bucketExists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(BUCKET).build());
            }

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(BUCKET)
                            .object(objectName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            return minioConfig.getEndpoint() + "/" + BUCKET + "/" + objectName;
        } catch (Exception e) {
            log.error("Error al subir imagen a MinIO", e);
            throw new RuntimeException("Fallo al subir la imagen", e);
        }
    }

    public void deleteImage(String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(BUCKET)
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            log.error("Error al eliminar imagen en MinIO", e);
            throw new RuntimeException("Fallo al eliminar la imagen", e);
        }
    }
}
