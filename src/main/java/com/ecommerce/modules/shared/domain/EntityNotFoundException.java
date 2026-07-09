package com.ecommerce.modules.shared.domain;

public class EntityNotFoundException extends RuntimeException {

    public EntityNotFoundException(String entityName, Object id) {
        super(String.format("%s with id '%s' not found", entityName, id));
    }

    public EntityNotFoundException(String message) {
        super(message);
    }
}
