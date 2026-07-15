package com.ecommerce.modulos.inquilino.domain;

import com.ecommerce.modulos.compartido.infrastructure.RepositorioJpaBase;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RepositorioPlanSuscripcion extends RepositorioJpaBase<PlanSuscripcion> {

    Optional<PlanSuscripcion> findByTipoPlanAndActiveTrue(TipoPlanSuscripcion tipoPlan);
}
