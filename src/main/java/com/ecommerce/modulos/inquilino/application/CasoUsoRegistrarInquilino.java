package com.ecommerce.modulos.inquilino.application;

import com.ecommerce.modulos.identidad.application.DetallesUsuarioPersonalizado;
import com.ecommerce.modulos.compartido.domain.ExcepcionViolacionReglaNegocio;
import com.ecommerce.modulos.compartido.domain.ExcepcionEntidadNoEncontrada;
import com.ecommerce.modulos.inquilino.application.dto.*;
import com.ecommerce.modulos.inquilino.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CasoUsoRegistrarInquilino {

    private final RepositorioInquilino repositorioInquilino;
    private final RepositorioPlanSuscripcion planRepository;
    private final RepositorioSuscripcion repositorioSuscripcion;

    @Transactional
    public RespuestaInquilino execute(SolicitudRegistrarInquilino request) {
        DetallesUsuarioPersonalizado userDetails = getCurrentUser();

        if (repositorioInquilino.existsByIdPropietario(userDetails.getUserId())) {
            throw new ExcepcionViolacionReglaNegocio("You already have a store registered");
        }

        if (repositorioInquilino.existsByEnlaceCorto(request.getEnlaceCorto())) {
            throw new ExcepcionViolacionReglaNegocio("Store enlaceCorto is already taken");
        }

        Inquilino inquilino = new Inquilino(
                request.getNombre(),
                request.getEnlaceCorto(),
                userDetails.getUserId()
        );
        inquilino.setDescripcion(request.getDescripcion());

        inquilino = repositorioInquilino.save(inquilino);

        PlanSuscripcion freePlan = planRepository.findByTipoPlanAndActiveTrue(TipoPlanSuscripcion.FREE)
                .orElseThrow(() -> new IllegalStateException("Default FREE plan not found"));

        Suscripcion suscripcion = new Suscripcion();
        suscripcion.setIdTienda(inquilino.getId());
        suscripcion.setIdPlan(freePlan.getId());
        suscripcion.setEstado("ACTIVE");
        suscripcion.setFechaInicio(LocalDateTime.now());
        repositorioSuscripcion.save(suscripcion);

        return mapToResponse(inquilino);
    }

    private RespuestaInquilino mapToResponse(Inquilino inquilino) {
        return RespuestaInquilino.builder()
                .id(inquilino.getId())
                .nombre(inquilino.getNombre())
                .enlaceCorto(inquilino.getEnlaceCorto())
                .descripcion(inquilino.getDescripcion())
                .urlLogo(inquilino.getUrlLogo())
                .urlBanner(inquilino.getUrlBanner())
                .estado(inquilino.getEstado().name())
                .idPropietario(inquilino.getIdPropietario())
                .creadoEn(inquilino.getCreadoEn())
                .build();
    }

    private DetallesUsuarioPersonalizado getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof DetallesUsuarioPersonalizado userDetails)) {
            throw new ExcepcionViolacionReglaNegocio("You must be authenticated to register a store");
        }
        return userDetails;
    }
}
