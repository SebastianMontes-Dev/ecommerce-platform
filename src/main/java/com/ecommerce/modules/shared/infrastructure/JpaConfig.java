package com.ecommerce.modules.shared.infrastructure;

import com.ecommerce.modules.shared.domain.DomainEvent;
import com.ecommerce.modules.shared.domain.DomainEventPublisher;
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
@EntityScan(basePackages = "com.ecommerce.modules")
@EnableJpaRepositories(basePackages = "com.ecommerce.modules")
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
