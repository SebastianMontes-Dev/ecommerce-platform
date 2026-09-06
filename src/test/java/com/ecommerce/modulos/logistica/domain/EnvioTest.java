package com.ecommerce.modulos.logistica.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EnvioTest {

    private Envio envio;
    private UUID idTienda;

    @BeforeEach
    void setUp() {
        idTienda = UUID.randomUUID();
        envio = new Envio();
        envio.setIdTienda(idTienda);
    }

    @Test
    void debeCambiarEstadoAlActualizar() {
        envio.actualizarEstado(EstadoEnvio.RECOLECTADO, "Bodega Central", "Paquete recolectado por el transportista");

        assertEquals(EstadoEnvio.RECOLECTADO, envio.getEstado());
    }

    @Test
    void debeAgregarUnEventoAlHistorialAlActualizarEstado() {
        envio.actualizarEstado(EstadoEnvio.RECOLECTADO, "Bodega Central", "Paquete recolectado por el transportista");

        assertEquals(1, envio.getHistorial().size());
    }

    @Test
    void eventoAgregadoDebeTenerLosDatosDeLaActualizacion() {
        envio.actualizarEstado(EstadoEnvio.EN_REPARTO, "Zona Norte", "Repartidor asignado");

        EventoTracking evento = envio.getHistorial().get(0);
        assertEquals(idTienda, evento.getIdTienda());
        assertEquals(EstadoEnvio.EN_REPARTO, evento.getEstado());
        assertEquals("Zona Norte", evento.getUbicacion());
        assertEquals("Repartidor asignado", evento.getDescripcion());
        assertSame(envio, evento.getEnvio());
    }

    @Test
    void debeAcumularMultiplesEventosEnElHistorialSinPisarLosAnteriores() {
        envio.actualizarEstado(EstadoEnvio.PREPARANDO, "Centro de Distribución", "Orden recibida para empaque");
        envio.actualizarEstado(EstadoEnvio.RECOLECTADO, "Bodega Central", "Paquete recolectado");
        envio.actualizarEstado(EstadoEnvio.EN_TRANSITO, "Autopista Sur", "En camino a destino");

        assertEquals(3, envio.getHistorial().size());
        assertEquals(EstadoEnvio.PREPARANDO, envio.getHistorial().get(0).getEstado());
        assertEquals(EstadoEnvio.RECOLECTADO, envio.getHistorial().get(1).getEstado());
        assertEquals(EstadoEnvio.EN_TRANSITO, envio.getHistorial().get(2).getEstado());
        assertEquals(EstadoEnvio.EN_TRANSITO, envio.getEstado());
    }
}
