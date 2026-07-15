package com.ecommerce.modulos.compartido.domain;

import java.util.Collections;
import java.util.List;

public class ExcepcionViolacionReglaNegocio extends RuntimeException {

    private final List<String> violations;

    public ExcepcionViolacionReglaNegocio(String message) {
        super(message);
        this.violations = List.of(message);
    }

    public ExcepcionViolacionReglaNegocio(List<String> violations) {
        super(String.join("; ", violations));
        this.violations = Collections.unmodifiableList(violations);
    }

    public List<String> getViolations() {
        return violations;
    }
}
