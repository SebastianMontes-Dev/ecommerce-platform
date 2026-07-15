package com.ecommerce.modulos.compartido.infrastructure;

import java.util.UUID;

public final class ContextoInquilino {

    private static final ThreadLocal<UUID> CURRENT_TENANT = new ThreadLocal<>();

    private ContextoInquilino() {}

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
