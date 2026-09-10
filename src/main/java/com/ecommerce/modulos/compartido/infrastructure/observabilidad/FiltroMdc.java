package com.ecommerce.modulos.compartido.infrastructure.observabilidad;

import com.ecommerce.modulos.compartido.infrastructure.ContextoInquilino;
import com.ecommerce.modulos.identidad.application.DetallesUsuarioPersonalizado;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Copia el inquilino y el usuario de la request al MDC de SLF4J para que <b>toda</b> línea de log
 * emitida durante el procesamiento los lleve, sin que cada punto de log tenga que pasarlos a mano.
 * En producción esos campos salen en el JSON estructurado (listo para ELK/Loki); en desarrollo
 * aparecen en el patrón de consola (ver {@code logging.pattern.correlation}).
 *
 * <p>{@code traceId}/{@code spanId} ya los inyecta Micrometer Tracing (puente Brave), así que
 * este filtro solo añade {@code tenantId} y {@code userId}.
 *
 * <p>Corre con {@code @Order(2)}: después de {@code FiltroInquilino} ({@code @Order(1)}), que es
 * quien resuelve el inquilino y lo deja en {@link ContextoInquilino} (un {@code ThreadLocal} que
 * sigue poblado porque {@code FiltroInquilino} lo limpia recién al desenrollarse la cadena).
 */
@Component
@Order(2)
public class FiltroMdc extends OncePerRequestFilter {

    static final String CLAVE_TENANT = "tenantId";
    static final String CLAVE_USUARIO = "userId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            ponerSiNoNulo(CLAVE_TENANT, ContextoInquilino.getIdTienda());
            ponerSiNoNulo(CLAVE_USUARIO, idUsuarioAutenticado());
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(CLAVE_TENANT);
            MDC.remove(CLAVE_USUARIO);
        }
    }

    private static void ponerSiNoNulo(String clave, UUID valor) {
        if (valor != null) {
            MDC.put(clave, valor.toString());
        }
    }

    private static UUID idUsuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && auth.getPrincipal() instanceof DetallesUsuarioPersonalizado detalles) {
            return detalles.getUserId();
        }
        return null;
    }
}
