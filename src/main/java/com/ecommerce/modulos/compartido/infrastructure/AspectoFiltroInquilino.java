package com.ecommerce.modulos.compartido.infrastructure;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

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
        if (idTienda == null) {
            return;
        }
        Session session = entityManager.unwrap(Session.class);
        ConfiguracionFiltroInquilinoHibernate.enableFilter(session, idTienda);
    }
}
