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
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/v1/pagos/webhook/**",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/actuator/**"
                );
    }
}
