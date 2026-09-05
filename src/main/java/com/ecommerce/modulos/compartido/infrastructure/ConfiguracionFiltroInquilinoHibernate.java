package com.ecommerce.modulos.compartido.infrastructure;

import org.hibernate.Session;

import java.util.UUID;

/**
 * Activa el filtro Hibernate "filtroInquilino" (declarado en EntidadInquilino)
 * sobre una Session. Se invoca desde AspectoFiltroInquilino antes de cada
 * acceso a repositorio — es la defensa de fondo del aislamiento multi-tenant,
 * independiente de que cada caso de uso filtre manualmente por idTienda.
 */
public final class ConfiguracionFiltroInquilinoHibernate {

    private ConfiguracionFiltroInquilinoHibernate() {}

    public static void enableFilter(Session session, UUID idTienda) {
        if (idTienda != null && session.getEnabledFilter("filtroInquilino") == null) {
            session.enableFilter("filtroInquilino")
                    .setParameter("idTienda", idTienda);
        }
    }
}
