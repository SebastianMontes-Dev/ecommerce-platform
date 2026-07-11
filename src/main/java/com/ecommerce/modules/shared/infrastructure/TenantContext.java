package com.ecommerce.modules.shared.infrastructure;

import java.util.UUID;

public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {}

    public static void setIdTienda(UUID idTienda) {
        CURRENT_TENANT.set(idTienda);
    }

    public static UUID getIdTienda() {
        return CURRENT_TENANT.get();
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
