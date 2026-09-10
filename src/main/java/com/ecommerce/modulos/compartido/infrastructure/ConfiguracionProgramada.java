package com.ecommerce.modulos.compartido.infrastructure;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Habilita las tareas {@code @Scheduled} (hoy: el worker del outbox, {@code ProcesadorOutbox}).
 */
@Configuration
@EnableScheduling
public class ConfiguracionProgramada {
}
