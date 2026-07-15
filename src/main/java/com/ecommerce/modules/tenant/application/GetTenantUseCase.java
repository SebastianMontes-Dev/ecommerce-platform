package com.ecommerce.modules.tenant.application;

import com.ecommerce.modules.shared.domain.EntityNotFoundException;
import com.ecommerce.modules.tenant.application.dto.TenantResponse;
import com.ecommerce.modules.tenant.domain.Tenant;
import com.ecommerce.modules.tenant.domain.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetTenantUseCase {

    private final TenantRepository tenantRepository;

    @Transactional(readOnly = true)
    public TenantResponse bySlug(String slug) {
        Tenant tenant = tenantRepository.findBySlug(slug)
                .orElseThrow(() -> new EntityNotFoundException("Tenant", slug));
        return mapToResponse(tenant);
    }

    @Transactional(readOnly = true)
    public TenantResponse myTenant() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof com.ecommerce.modules.identity.application.CustomUserDetails userDetails)) {
            throw new EntityNotFoundException("Tenant not found - not authenticated");
        }

        Tenant tenant = tenantRepository.findByIdPropietario(userDetails.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found for current user"));
        return mapToResponse(tenant);
    }

    private TenantResponse mapToResponse(Tenant tenant) {
        return TenantResponse.builder()
                .id(tenant.getId())
                .nombre(tenant.getNombre())
                .slug(tenant.getSlug())
                .descripcion(tenant.getDescripcion())
                .urlLogo(tenant.getUrlLogo())
                .urlBanner(tenant.getUrlBanner())
                .estado(tenant.getEstado().name())
                .idPropietario(tenant.getIdPropietario())
                .creadoEn(tenant.getCreadoEn())
                .build();
    }
}
