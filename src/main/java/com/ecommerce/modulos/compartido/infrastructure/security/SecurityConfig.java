package com.ecommerce.modulos.compartido.infrastructure.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final FiltroAutenticacionJwt filtroAutenticacionJwt;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final ManejadorExitoAutenticacionOAuth2 manejadorExitoAutenticacionOAuth2;
    private final ManejadorFalloAutenticacionOAuth2 manejadorFalloAutenticacionOAuth2;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/auth/**",
                                "/api/v1/pagos/webhook/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/api-docs/**",
                                "/actuator/health/**",
                                // El handshake SockJS no admite cabeceras: la autenticación se
                                // hace en el frame STOMP CONNECT (InterceptorAutenticacionWebSocket).
                                "/ws/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/catalogo/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/busqueda/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/inquilinos/{enlaceCorto}").permitAll()
                        .requestMatchers("/actuator/**").hasRole("PLATFORM_ADMIN")
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )
                        // Sin esto, un login social exitoso terminaba en un callejón sin salida:
                        // CustomOAuth2UserService crea/encuentra al Usuario, pero el contexto
                        // STATELESS se descarta al final de la request sin emitir el JWT que el
                        // resto de la API necesita.
                        .successHandler(manejadorExitoAutenticacionOAuth2)
                        .failureHandler(manejadorFalloAutenticacionOAuth2)
                )
                .addFilterBefore(filtroAutenticacionJwt, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("http://localhost:*", "https://*.ecommerce.com"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Inquilino-ID"));
        configuration.setExposedHeaders(Arrays.asList("X-RateLimit-Limit", "X-RateLimit-Remaining"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}
