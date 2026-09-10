package com.ecommerce.modulos.compartido.infrastructure.observabilidad;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Contadores de negocio expuestos vía Actuator/Prometheus ({@code /actuator/prometheus}).
 * Complementan las métricas técnicas que Micrometer ya trae (HTTP, JVM, HikariCP) con señales
 * que le importan al negocio y a las alertas: cuántas órdenes entran, cuántos pagos se caen,
 * cuántas veces el stock deja a un cliente sin comprar.
 *
 * <p>Sin etiqueta por inquilino a propósito: la cardinalidad de {@code tenant_id} en Prometheus
 * crecería sin control. Para desglose por tienda está el reporte de analíticas sobre Postgres.
 */
@Component
public class MetricasNegocio {

    private final Counter ordenesCreadas;
    private final Counter pagosFallidos;
    private final Counter inventarioAgotado;

    public MetricasNegocio(MeterRegistry registro) {
        this.ordenesCreadas = Counter.builder("ordenes.creadas")
                .description("Órdenes creadas y confirmadas desde el carrito (post-commit)")
                .register(registro);
        this.pagosFallidos = Counter.builder("pagos.fallidos")
                .description("Pagos que no prosperaron: sesión de Stripe expirada o rechazo de la pasarela")
                .register(registro);
        this.inventarioAgotado = Counter.builder("inventario.agotado")
                .description("Reservas de inventario abortadas por stock insuficiente")
                .register(registro);
    }

    public void ordenCreada() {
        ordenesCreadas.increment();
    }

    public void pagoFallido() {
        pagosFallidos.increment();
    }

    public void inventarioAgotado() {
        inventarioAgotado.increment();
    }
}
