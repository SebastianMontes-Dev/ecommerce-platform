package com.ecommerce.modulos.inquilino.application;

import com.ecommerce.modulos.compartido.domain.ExcepcionEntidadNoEncontrada;
import com.ecommerce.modulos.inquilino.application.dto.RespuestaInquilino;
import com.ecommerce.modulos.inquilino.domain.Inquilino;
import com.ecommerce.modulos.inquilino.domain.RepositorioInquilino;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CasoUsoObtenerInquilino {

    private final RepositorioInquilino repositorioInquilino;

    @Transactional(readOnly = true)
    public RespuestaInquilino bySlug(String enlaceCorto) {
        Inquilino inquilino = repositorioInquilino.findByEnlaceCorto(enlaceCorto)
                .orElseThrow(() -> new ExcepcionEntidadNoEncontrada("Inquilino", enlaceCorto));
        return mapToResponse(inquilino);
    }

    @Transactional(readOnly = true)
    public RespuestaInquilino myTenant() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof com.ecommerce.modulos.identidad.application.DetallesUsuarioPersonalizado userDetails)) {
            throw new ExcepcionEntidadNoEncontrada("Inquilino not found - not authenticated");
        }

        Inquilino inquilino = repositorioInquilino.findByIdPropietario(userDetails.getUserId())
                .orElseThrow(() -> new ExcepcionEntidadNoEncontrada("Inquilino not found for current usuario"));
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
}
