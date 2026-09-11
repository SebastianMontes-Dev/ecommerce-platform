package com.ecommerce.modulos.compartido.domain;

/**
 * Un servicio externo (pasarela de IA, SMTP, un webhook de terceros, etc.) no respondió o
 * respondió con error. Se distingue de {@link ExcepcionOperacionInvalida} porque la causa no
 * es un dato de negocio inválido sino una dependencia fuera de nuestro control — el mensaje
 * de esta excepción es para logs internos; {@link com.ecommerce.modulos.compartido.infrastructure.ManejadorExcepcionGlobal}
 * nunca lo reenvía tal cual al cliente.
 */
public class ExcepcionServicioExterno extends RuntimeException {

    public ExcepcionServicioExterno(String message, Throwable cause) {
        super(message, cause);
    }
}
