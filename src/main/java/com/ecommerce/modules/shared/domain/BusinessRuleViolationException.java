package com.ecommerce.modules.shared.domain;

import java.util.Collections;
import java.util.List;

public class BusinessRuleViolationException extends RuntimeException {

    private final List<String> violations;

    public BusinessRuleViolationException(String message) {
        super(message);
        this.violations = List.of(message);
    }

    public BusinessRuleViolationException(List<String> violations) {
        super(String.join("; ", violations));
        this.violations = Collections.unmodifiableList(violations);
    }

    public List<String> getViolations() {
        return violations;
    }
}
