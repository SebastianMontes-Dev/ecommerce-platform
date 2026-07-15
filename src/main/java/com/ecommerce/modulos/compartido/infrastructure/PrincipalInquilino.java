package com.ecommerce.modulos.compartido.infrastructure;

import java.util.UUID;

public class PrincipalInquilino {

    private final UUID idTienda;

    public PrincipalInquilino(UUID idTienda) {
        this.idTienda = idTienda;
    }

    public UUID getIdTienda() {
        return idTienda;
    }
}
