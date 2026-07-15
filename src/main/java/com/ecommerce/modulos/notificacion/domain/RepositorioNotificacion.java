package com.ecommerce.modulos.notificacion.domain;

import com.ecommerce.modulos.compartido.infrastructure.RepositorioJpaBase;
import org.springframework.stereotype.Repository;

@Repository
public interface RepositorioNotificacion extends RepositorioJpaBase<Notificacion> {
}
