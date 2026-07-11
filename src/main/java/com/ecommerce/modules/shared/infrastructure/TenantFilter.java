package com.ecommerce.modules.shared.infrastructure;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(1)
public class TenantFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TenantFilter.class);
    private static final String TENANT_HEADER = "X-Tenant-ID";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            UUID idTienda = extractTenantId(request);
            if (idTienda != null) {
                TenantContext.setIdTienda(idTienda);
                log.debug("Tenant context set to: {}", idTienda);
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private UUID extractTenantId(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof TenantPrincipal tenantPrincipal) {
            return tenantPrincipal.getIdTienda();
        }

        String header = request.getHeader(TENANT_HEADER);
        if (header != null && !header.isBlank()) {
            try {
                return UUID.fromString(header);
            } catch (IllegalArgumentException e) {
                log.debug("Invalid X-Tenant-ID header format: {}", header);
            }
        }

        return null;
    }
}
