package com.ecommerce.modules.tenant.application;

import com.ecommerce.modules.identity.application.CustomUserDetails;
import com.ecommerce.modules.shared.domain.BusinessRuleViolationException;
import com.ecommerce.modules.shared.domain.EntityNotFoundException;
import com.ecommerce.modules.tenant.application.dto.*;
import com.ecommerce.modules.tenant.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RegisterTenantUseCase {

    private final TenantRepository tenantRepository;
    private final SubscriptionPlanRepository planRepository;
    private final SubscriptionRepository subscriptionRepository;

    @Transactional
    public TenantResponse execute(RegisterTenantRequest request) {
        CustomUserDetails userDetails = getCurrentUser();

        if (tenantRepository.existsByIdPropietario(userDetails.getUserId())) {
            throw new BusinessRuleViolationException("You already have a store registered");
        }

        if (tenantRepository.existsBySlug(request.getSlug())) {
            throw new BusinessRuleViolationException("Store slug is already taken");
        }

        Tenant tenant = new Tenant(
                request.getNombre(),
                request.getSlug(),
                userDetails.getUserId()
        );
        tenant.setDescripcion(request.getDescripcion());

        tenant = tenantRepository.save(tenant);

        SubscriptionPlan freePlan = planRepository.findByTipoPlanAndActiveTrue(SubscriptionPlanType.FREE)
                .orElseThrow(() -> new IllegalStateException("Default FREE plan not found"));

        Subscription subscription = new Subscription();
        subscription.setIdTienda(tenant.getId());
        subscription.setIdPlan(freePlan.getId());
        subscription.setEstado("ACTIVE");
        subscription.setFechaInicio(LocalDateTime.now());
        subscriptionRepository.save(subscription);

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

    private CustomUserDetails getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new BusinessRuleViolationException("You must be authenticated to register a store");
        }
        return userDetails;
    }
}
