package com.ecommerce.modulos.compartido.infrastructure;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class ConfiguracionObservabilidad {
    @PostConstruct
    public void init() {
        log.info("Tracing distribuido inicializado con Zipkin y Micrometer.");
    }
}
