package com.ecommerce.modulos.compartido.infrastructure.observabilidad;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MetricasNegocioTest {

    private SimpleMeterRegistry registro;
    private MetricasNegocio metricas;

    @BeforeEach
    void setUp() {
        registro = new SimpleMeterRegistry();
        metricas = new MetricasNegocio(registro);
    }

    @Test
    void cadaMetodoIncrementaSuContadorPorNombre() {
        metricas.ordenCreada();
        metricas.ordenCreada();
        metricas.pagoFallido();
        metricas.inventarioAgotado();

        assertEquals(2.0, registro.get("ordenes.creadas").counter().count());
        assertEquals(1.0, registro.get("pagos.fallidos").counter().count());
        assertEquals(1.0, registro.get("inventario.agotado").counter().count());
    }

    @Test
    void losContadoresArrancanEnCero() {
        assertEquals(0.0, registro.get("ordenes.creadas").counter().count());
        assertEquals(0.0, registro.get("pagos.fallidos").counter().count());
        assertEquals(0.0, registro.get("inventario.agotado").counter().count());
    }
}
