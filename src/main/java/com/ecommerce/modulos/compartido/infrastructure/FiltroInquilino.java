package com.ecommerce.modulos.compartido.infrastructure;

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
public class FiltroInquilino extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(FiltroInquilino.class);
    private static final String TENANT_HEADER = "X-Inquilino-ID";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            UUID idTienda = extractTenantId(request);
            if (idTienda != null) {
                ContextoInquilino.setIdTienda(idTienda);
                log.debug("Inquilino context set to: {}", idTienda);
            }
            filterChain.doFilter(request, response);
        } finally {
            ContextoInquilino.clear();
        }
    }

    private UUID extractTenantId(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof PrincipalInquilino principalInquilino) {
            return principalInquilino.getIdTienda();
        }

        String header = request.getHeader(TENANT_HEADER);
        if (header != null && !header.isBlank()) {
            try {
                return UUID.fromString(header);
            } catch (IllegalArgumentException e) {
                log.debug("Invalid X-Inquilino-ID header format: {}", header);
            }
        }

        return null;
    }
}
