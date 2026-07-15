package com.ecommerce.modulos.compartido.domain;

public class ExcepcionEntidadNoEncontrada extends RuntimeException {

    public ExcepcionEntidadNoEncontrada(String entityName, Object id) {
        super(String.format("%s with id '%s' not found", entityName, id));
    }

    public ExcepcionEntidadNoEncontrada(String message) {
        super(message);
    }
}
