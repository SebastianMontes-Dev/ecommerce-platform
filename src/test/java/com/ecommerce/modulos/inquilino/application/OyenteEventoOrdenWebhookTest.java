package com.ecommerce.modulos.inquilino.application;

import com.ecommerce.modulos.ordenes.domain.ArticuloOrden;
import com.ecommerce.modulos.ordenes.domain.EstadoOrden;
import com.ecommerce.modulos.ordenes.domain.events.EventoOrdenCreada;
import com.ecommerce.modulos.ordenes.domain.eventos.EventoEstadoOrdenCambiado;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OyenteEventoOrdenWebhookTest {

    @Mock
    private ServicioEmisorWebhook servicioEmisorWebhook;

    private ObjectMapper objectMapper;

    private OyenteEventoOrdenWebhook oyenteEventoOrdenWebhook;

    private UUID idOrden;
    private UUID idTienda;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        oyenteEventoOrdenWebhook = new OyenteEventoOrdenWebhook(servicioEmisorWebhook, objectMapper);
        idOrden = UUID.randomUUID();
        idTienda = UUID.randomUUID();
    }

    @Test
    void debeEmitirEventoOrdenCreadaConIdOrdenIdTiendaEIdCliente() throws Exception {
        UUID idCliente = UUID.randomUUID();
        ArticuloOrden articulo = new ArticuloOrden();
        articulo.setIdProducto(UUID.randomUUID());
        articulo.setCantidad(2);
        EventoOrdenCreada evento = new EventoOrdenCreada(idOrden, idTienda, idCliente, List.of(articulo));

        oyenteEventoOrdenWebhook.onOrderCreated(evento);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(servicioEmisorWebhook).emitirEvento(eq(idTienda), eq("orden.creada"), payloadCaptor.capture());

        JsonNode payload = objectMapper.readTree(payloadCaptor.getValue());
        assertEquals(idOrden.toString(), payload.get("idOrden").asText());
        assertEquals(idTienda.toString(), payload.get("idTienda").asText());
        assertEquals(idCliente.toString(), payload.get("idCliente").asText());
    }

    @Test
    void debeEmitirOrdenConfirmadaCuandoEstadoCambiaAConfirmed() throws Exception {
        EventoEstadoOrdenCambiado evento = new EventoEstadoOrdenCambiado(
                idOrden, idTienda, EstadoOrden.PENDING, EstadoOrden.CONFIRMED, null);

        oyenteEventoOrdenWebhook.onOrderStatusChanged(evento);

        verify(servicioEmisorWebhook).emitirEvento(eq(idTienda), eq("orden.confirmada"), anyString());
    }

    @Test
    void debeEmitirOrdenPagadaCuandoEstadoCambiaAPaid() {
        EventoEstadoOrdenCambiado evento = new EventoEstadoOrdenCambiado(
                idOrden, idTienda, EstadoOrden.CONFIRMED, EstadoOrden.PAID, null);

        oyenteEventoOrdenWebhook.onOrderStatusChanged(evento);

        verify(servicioEmisorWebhook).emitirEvento(eq(idTienda), eq("orden.pagada"), anyString());
    }

    @Test
    void debeEmitirOrdenEnviadaCuandoEstadoCambiaAShipped() {
        EventoEstadoOrdenCambiado evento = new EventoEstadoOrdenCambiado(
                idOrden, idTienda, EstadoOrden.PAID, EstadoOrden.SHIPPED, null);

        oyenteEventoOrdenWebhook.onOrderStatusChanged(evento);

        verify(servicioEmisorWebhook).emitirEvento(eq(idTienda), eq("orden.enviada"), anyString());
    }

    @Test
    void debeEmitirOrdenEntregadaCuandoEstadoCambiaADelivered() {
        EventoEstadoOrdenCambiado evento = new EventoEstadoOrdenCambiado(
                idOrden, idTienda, EstadoOrden.SHIPPED, EstadoOrden.DELIVERED, null);

        oyenteEventoOrdenWebhook.onOrderStatusChanged(evento);

        verify(servicioEmisorWebhook).emitirEvento(eq(idTienda), eq("orden.entregada"), anyString());
    }

    @Test
    void debeEmitirOrdenCanceladaCuandoEstadoCambiaACancelled() {
        EventoEstadoOrdenCambiado evento = new EventoEstadoOrdenCambiado(
                idOrden, idTienda, EstadoOrden.PENDING, EstadoOrden.CANCELLED, "motivo");

        oyenteEventoOrdenWebhook.onOrderStatusChanged(evento);

        verify(servicioEmisorWebhook).emitirEvento(eq(idTienda), eq("orden.cancelada"), anyString());
    }

    @Test
    void debeEmitirOrdenReembolsadaCuandoEstadoCambiaARefunded() {
        EventoEstadoOrdenCambiado evento = new EventoEstadoOrdenCambiado(
                idOrden, idTienda, EstadoOrden.CANCELLED, EstadoOrden.REFUNDED, null);

        oyenteEventoOrdenWebhook.onOrderStatusChanged(evento);

        verify(servicioEmisorWebhook).emitirEvento(eq(idTienda), eq("orden.reembolsada"), anyString());
    }

    @ParameterizedTest
    @EnumSource(value = EstadoOrden.class, names = {"PENDING", "PROCESSING"})
    void noDebeEmitirNingunWebhookParaEstadosSinEventoConfigurado(EstadoOrden nuevoEstado) {
        EventoEstadoOrdenCambiado evento = new EventoEstadoOrdenCambiado(
                idOrden, idTienda, EstadoOrden.PENDING, nuevoEstado, null);

        oyenteEventoOrdenWebhook.onOrderStatusChanged(evento);

        verifyNoInteractions(servicioEmisorWebhook);
    }

    @Test
    void noDebePropagarExcepcionSiFallaLaSerializacionDelPayload() throws Exception {
        ObjectMapper objectMapperQueFalla = mock(ObjectMapper.class);
        when(objectMapperQueFalla.writeValueAsString(any()))
                .thenThrow(new com.fasterxml.jackson.core.JsonProcessingException("fallo simulado") {});
        OyenteEventoOrdenWebhook listener = new OyenteEventoOrdenWebhook(servicioEmisorWebhook, objectMapperQueFalla);
        EventoOrdenCreada evento = new EventoOrdenCreada(idOrden, idTienda, UUID.randomUUID(), List.of());

        assertDoesNotThrow(() -> listener.onOrderCreated(evento));

        verifyNoInteractions(servicioEmisorWebhook);
    }
}
