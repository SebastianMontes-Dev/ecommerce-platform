package com.ecommerce.modulos.compartido.infrastructure;

import com.ecommerce.modulos.compartido.domain.ExcepcionNoAutorizado;

import java.util.UUID;

public final class ContextoInquilino {

    private static final ThreadLocal<UUID> CURRENT_TENANT = new ThreadLocal<>();
    private static final ThreadLocal<UUID> OWNED_TENANT = new ThreadLocal<>();

    private ContextoInquilino() {}

    public static void setIdTienda(UUID idTienda) {
        CURRENT_TENANT.set(idTienda);
    }

    public static UUID getIdTienda() {
        return CURRENT_TENANT.get();
    }

    /**
     * Tienda que el usuario autenticado posee (Inquilino.idPropietario), resuelta
     * server-side vía ServicioResolutorInquilino. Nunca proviene de un header.
     */
    public static void setIdTiendaPropia(UUID idTienda) {
        OWNED_TENANT.set(idTienda);
    }

    /**
     * Para endpoints de gestión de tienda. Lanza si el usuario autenticado no
     * es dueño de ninguna tienda — nunca cae de vuelta al header del cliente.
     */
    public static UUID getIdTiendaPropia() {
        UUID idTienda = OWNED_TENANT.get();
        if (idTienda == null) {
            throw new ExcepcionNoAutorizado("No tienes una tienda registrada");
        }
        return idTienda;
    }

    public static void clear() {
        CURRENT_TENANT.remove();
        OWNED_TENANT.remove();
    }
}
