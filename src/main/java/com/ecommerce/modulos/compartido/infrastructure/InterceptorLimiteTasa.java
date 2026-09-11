package com.ecommerce.modulos.compartido.infrastructure;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class InterceptorLimiteTasa implements HandlerInterceptor {

    private final StringRedisTemplate redisTemplate;

    private static final int MAX_REQUESTS_PER_MINUTE = 60;
    private static final int MAX_AUTH_REQUESTS_PER_MINUTE = 10;
    // Antes vivía en FiltroRateLimit (Bucket4j, Map en memoria): no se compartía entre
    // instancias -rompía el escalado horizontal- y el mapa crecía sin límite ni expiración.
    // Mismo umbral que tenía, ahora sobre el mismo Redis que ya usan los demás cubos.
    private static final int MAX_SENSITIVE_REQUESTS_PER_MINUTE = 10;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String clientIp = getClientIp(request);
        String path = request.getRequestURI();

        // El endpoint real de alta es /api/v1/auth/registro (no /register): con el prefijo
        // equivocado el registro caía al cubo general de 60/min en vez del estricto de 10/min.
        boolean isAuthEndpoint = path.startsWith("/api/v1/auth/login") || path.startsWith("/api/v1/auth/registro");
        boolean isSensitiveEndpoint = path.startsWith("/api/v1/ordenes/checkout") || path.startsWith("/api/v1/chatbot/chat");

        int maxRequests;
        String keyPrefix;
        if (isAuthEndpoint) {
            maxRequests = MAX_AUTH_REQUESTS_PER_MINUTE;
            keyPrefix = "rate:auth:";
        } else if (isSensitiveEndpoint) {
            maxRequests = MAX_SENSITIVE_REQUESTS_PER_MINUTE;
            keyPrefix = "rate:sensible:";
        } else {
            maxRequests = MAX_REQUESTS_PER_MINUTE;
            keyPrefix = "rate:api:";
        }

        String key = keyPrefix + clientIp;

        Long currentCount = redisTemplate.opsForValue().increment(key);

        if (currentCount != null && currentCount == 1) {
            redisTemplate.expire(key, Duration.ofMinutes(1));
        }

        if (currentCount != null && currentCount > maxRequests) {
            log.warn("Rate limit exceeded for IP: {} on path: {}", clientIp, path);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/problem+json");
            response.getWriter().write("""
                    {
                        "type": "https://api.ecommerce.com/errors/rate-limit",
                        "title": "Too Many Requests",
                        "estado": 429,
                        "detail": "Has excedido el límite de peticiones. Intenta de nuevo en un momento."
                    }
                    """);
            return false;
        }

        response.setHeader("X-RateLimit-Limit", String.valueOf(maxRequests));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, maxRequests - (currentCount != null ? currentCount : 0))));

        return true;
    }

    /**
     * {@code request.getRemoteAddr()} y nada más: leer {@code X-Forwarded-For}/{@code X-Real-IP}
     * acá directamente permitía que cualquier cliente mandara su propio valor y reseteara su
     * cubo a voluntad, neutralizando los tres límites de este interceptor. En producción
     * ({@code server.forward-headers-strategy: native}, ver {@code application-prod.yml}) es
     * Tomcat ({@code RemoteIpValve}) quien resuelve {@code getRemoteAddr()} a partir de
     * {@code X-Forwarded-For}, y solo cuando la conexión entra desde un proxy confiable — un
     * cliente que no pasa por ese proxy no puede falsear nada acá.
     */
    private String getClientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
