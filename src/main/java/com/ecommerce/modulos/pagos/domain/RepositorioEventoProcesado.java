package com.ecommerce.modulos.pagos.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioEventoProcesado extends JpaRepository<EventoProcesado, String> {
}
