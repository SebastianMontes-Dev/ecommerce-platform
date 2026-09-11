package com.ecommerce.modulos.compartido.infrastructure;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class RateLimitConfig implements WebMvcConfigurer {

    private final InterceptorLimiteTasa interceptorLimiteTasa;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptorLimiteTasa)
                // /graphql expone el mismo tipo de lectura que el catálogo REST y no tenía
                // ningún límite de tasa: quedaba fuera de "/api/**".
                .addPathPatterns("/api/**", "/graphql")
                .excludePathPatterns(
                        "/api/v1/pagos/webhook/**",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/actuator/**"
                );
    }
}
