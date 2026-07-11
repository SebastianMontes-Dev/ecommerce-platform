package com.ecommerce.modules.shared.infrastructure;

import java.util.UUID;

public class TenantPrincipal {

    private final UUID idTienda;

    public TenantPrincipal(UUID idTienda) {
        this.idTienda = idTienda;
    }

    public UUID getIdTienda() {
        return idTienda;
    }
}
