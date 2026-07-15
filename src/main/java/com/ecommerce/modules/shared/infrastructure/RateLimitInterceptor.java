package com.ecommerce.modules.shared.infrastructure;

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
public class RateLimitInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate redisTemplate;

    private static final int MAX_REQUESTS_PER_MINUTE = 60;
    private static final int MAX_AUTH_REQUESTS_PER_MINUTE = 10;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String clientIp = getClientIp(request);
        String path = request.getRequestURI();

        boolean isAuthEndpoint = path.startsWith("/api/v1/auth/login") || path.startsWith("/api/v1/auth/register");
        int maxRequests = isAuthEndpoint ? MAX_AUTH_REQUESTS_PER_MINUTE : MAX_REQUESTS_PER_MINUTE;
        String keyPrefix = isAuthEndpoint ? "rate:auth:" : "rate:api:";

        String key = keyPrefix + clientIp;

        Long currentCount = redisTemplate.opsForValue().increment(key);

        if (currentCount != null && currentCount == 1) {
            redisTemplate.expire(key, Duration.ofMinutes(1));
        }

        if (currentCount != null && currentCount > maxRequests) {
            log.warn("Rate limit exceeded for IP: {} on path: {}", clientIp, path);
            response.setEstado(HttpStatus.TOO_MANY_REQUESTS.value());
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

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
