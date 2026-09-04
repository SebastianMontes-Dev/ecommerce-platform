package com.ecommerce.modulos.compartido.infrastructure;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Session;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

/**
 * Activa el filtro Hibernate de aislamiento por tenant antes de cualquier
 * llamada a un repositorio Spring Data. Corre dentro de la transacción del
 * caso de uso/servicio que invoca al repositorio, por lo que el EntityManager
 * ya está vinculado a una Session activa.
 */
@Aspect
@Component
public class AspectoFiltroInquilino {

    @PersistenceContext
    private EntityManager entityManager;

    @Before("execution(* com.ecommerce.modulos.compartido.infrastructure.RepositorioJpaBase+.*(..))")
    public void activarFiltroInquilino() {
        UUID idTienda = ContextoInquilino.getIdTienda();
        // Con spring.jpa.open-in-view=false no hay persistence context de request.
        // Fuera de una transacción activa, entityManager.unwrap(Session.class) puede
        // devolver una Session temporal ya cerrada (o distinta a la que realmente usará
        // el repositorio), así que solo activamos el filtro cuando hay una transacción
        // Spring real en curso — el filtrado manual por idTienda en cada repositorio
        // sigue siendo la defensa primaria; este aspecto es el respaldo transaccional.
        if (idTienda == null || !TransactionSynchronizationManager.isActualTransactionActive()) {
            return;
        }
        Session session = entityManager.unwrap(Session.class);
        ConfiguracionFiltroInquilinoHibernate.enableFilter(session, idTienda);
    }
}
