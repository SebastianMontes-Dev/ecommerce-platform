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

        if (tenantRepository.existsByOwnerId(userDetails.getUserId())) {
            throw new BusinessRuleViolationException("You already have a store registered");
        }

        if (tenantRepository.existsBySlug(request.getSlug())) {
            throw new BusinessRuleViolationException("Store slug is already taken");
        }

        Tenant tenant = new Tenant(
                request.getName(),
                request.getSlug(),
                userDetails.getUserId()
        );
        tenant.setDescription(request.getDescription());

        tenant = tenantRepository.save(tenant);

        SubscriptionPlan freePlan = planRepository.findByPlanTypeAndActiveTrue(SubscriptionPlanType.FREE)
                .orElseThrow(() -> new IllegalStateException("Default FREE plan not found"));

        Subscription subscription = new Subscription();
        subscription.setTenantId(tenant.getId());
        subscription.setPlanId(freePlan.getId());
        subscription.setStatus("ACTIVE");
        subscription.setStartDate(LocalDateTime.now());
        subscriptionRepository.save(subscription);

        return mapToResponse(tenant);
    }

    private TenantResponse mapToResponse(Tenant tenant) {
        return TenantResponse.builder()
                .id(tenant.getId())
                .name(tenant.getName())
                .slug(tenant.getSlug())
                .description(tenant.getDescription())
                .logoUrl(tenant.getLogoUrl())
                .bannerUrl(tenant.getBannerUrl())
                .status(tenant.getStatus().name())
                .ownerId(tenant.getOwnerId())
                .createdAt(tenant.getCreatedAt())
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
