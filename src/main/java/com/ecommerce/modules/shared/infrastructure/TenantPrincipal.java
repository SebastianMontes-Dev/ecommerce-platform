package com.ecommerce.modules.shared.infrastructure;

import java.util.UUID;

public class TenantPrincipal {

    private final UUID tenantId;

    public TenantPrincipal(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public UUID getTenantId() {
        return tenantId;
    }
}
