package com.ecommerce.modulos.compartido.infrastructure;

import com.ecommerce.modulos.compartido.domain.EventoDominio;
import com.ecommerce.modulos.compartido.domain.PublicadorEventoDominio;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

@Configuration
@EntityScan(basePackages = "com.ecommerce.modulos")
@EnableJpaRepositories(basePackages = "com.ecommerce.modulos")
@EnableJpaAuditing
public class JpaConfig {

    @Bean
    public AuditorAware<UUID> auditorProvider() {
        return () -> Optional.empty();
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
