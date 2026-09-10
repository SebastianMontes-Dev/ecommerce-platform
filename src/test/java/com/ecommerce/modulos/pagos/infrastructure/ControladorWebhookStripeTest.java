package com.ecommerce.modulos.pagos.infrastructure;

import com.ecommerce.modulos.logistica.application.CasoUsoLogistica;
import com.ecommerce.modulos.pagos.application.CasoUsoConfirmarPago;
import com.ecommerce.modulos.pagos.application.CasoUsoRegistrarPagoFallido;
import com.ecommerce.modulos.pagos.domain.EstadoPago;
import com.ecommerce.modulos.pagos.domain.Pago;
import com.ecommerce.modulos.pagos.domain.RepositorioEventoProcesado;
import com.ecommerce.modulos.pagos.domain.RepositorioPago;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ControladorWebhookStripeTest {

    @Mock private RepositorioPago repositorioPago;
    @Mock private RepositorioEventoProcesado repositorioEventoProcesado;
    @Mock private CasoUsoConfirmarPago casoUsoConfirmarPago;
    @Mock private CasoUsoRegistrarPagoFallido casoUsoRegistrarPagoFallido;
    @Mock private CasoUsoLogistica casoUsoLogistica;

    @Mock private HttpServletRequest request;
    @Mock private Event evento;
    @Mock private EventDataObjectDeserializer deserializer;
    @Mock private Session session;

    private ControladorWebhookStripe controlador;

    private UUID idPago;
    private UUID idTienda;
    private UUID idOrden;

    @BeforeEach
    void setUp() throws Exception {
        controlador = new ControladorWebhookStripe(
                repositorioPago, repositorioEventoProcesado, casoUsoConfirmarPago,
                casoUsoRegistrarPagoFallido, casoUsoLogistica);
        ReflectionTestUtils.setField(controlador, "webhookSecret", "whsec_test");
        ReflectionTestUtils.setField(controlador, "stripeApiKey", "sk_test");

        idPago = UUID.randomUUID();
        idTienda = UUID.randomUUID();
        idOrden = UUID.randomUUID();

        when(request.getReader()).thenReturn(new BufferedReader(new StringReader("{}")));
        when(request.getHeader("Stripe-Signature")).thenReturn("sig");
    }

    private Pago pagoPendiente() {
        Pago pago = new Pago();
        pago.setId(idPago);
        pago.setIdTienda(idTienda);
        pago.setIdOrden(idOrden);
        pago.setEstado(EstadoPago.PENDING);
        pago.setIdExterno("cs_ref");
        return pago;
    }

    private void stubEventoCompletado() {
        when(evento.getId()).thenReturn("evt_1");
        when(evento.getType()).thenReturn("checkout.session.completed");
        when(evento.getDataObjectDeserializer()).thenReturn(deserializer);
        when(deserializer.getObject()).thenReturn(Optional.of((StripeObject) session));
        when(session.getClientReferenceId()).thenReturn("cs_ref");
        when(session.getPaymentIntent()).thenReturn("pi_123");
    }

    @Test
    @DisplayName("checkout.session.completed: confirma el pago atómicamente, prepara envío y responde 200")
    void pagoCompletadoOk() {
        try (MockedStatic<Webhook> webhook = mockStatic(Webhook.class)) {
            webhook.when(() -> Webhook.constructEvent(any(), any(), any())).thenReturn(evento);
            stubEventoCompletado();
            when(repositorioEventoProcesado.existsById("evt_1")).thenReturn(false);
            when(repositorioPago.findByIdExternoSinFiltro("cs_ref")).thenReturn(Optional.of(pagoPendiente()));
            when(casoUsoConfirmarPago.confirmarPagoExitoso(idPago, "pi_123")).thenReturn(idOrden);

            ResponseEntity<String> resp = controlador.handleStripeWebhook(request);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(casoUsoConfirmarPago).confirmarPagoExitoso(idPago, "pi_123");
            verify(casoUsoLogistica).prepararEnvio(idTienda, idOrden);
            verify(repositorioEventoProcesado).save(any());
        }
    }

    @Test
    @DisplayName("Si preparar el envío falla, el webhook igual responde 200 y marca el evento como procesado")
    void falloDeLogisticaNoRompeElWebhook() {
        try (MockedStatic<Webhook> webhook = mockStatic(Webhook.class)) {
            webhook.when(() -> Webhook.constructEvent(any(), any(), any())).thenReturn(evento);
            stubEventoCompletado();
            when(repositorioEventoProcesado.existsById("evt_1")).thenReturn(false);
            when(repositorioPago.findByIdExternoSinFiltro("cs_ref")).thenReturn(Optional.of(pagoPendiente()));
            when(casoUsoConfirmarPago.confirmarPagoExitoso(idPago, "pi_123")).thenReturn(idOrden);
            doThrow(new RuntimeException("DHL caído")).when(casoUsoLogistica).prepararEnvio(any(), any());

            ResponseEntity<String> resp = controlador.handleStripeWebhook(request);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(repositorioEventoProcesado).save(any());
        }
    }

    @Test
    @DisplayName("checkout.session.expired: registra el pago como fallido (que cancela la orden y repone stock)")
    void checkoutExpirado() {
        try (MockedStatic<Webhook> webhook = mockStatic(Webhook.class)) {
            webhook.when(() -> Webhook.constructEvent(any(), any(), any())).thenReturn(evento);
            when(evento.getId()).thenReturn("evt_exp");
            when(evento.getType()).thenReturn("checkout.session.expired");
            when(evento.getDataObjectDeserializer()).thenReturn(deserializer);
            when(deserializer.getObject()).thenReturn(Optional.of((StripeObject) session));
            when(session.getClientReferenceId()).thenReturn("cs_ref");
            when(repositorioEventoProcesado.existsById("evt_exp")).thenReturn(false);
            when(repositorioPago.findByIdExternoSinFiltro("cs_ref")).thenReturn(Optional.of(pagoPendiente()));

            ResponseEntity<String> resp = controlador.handleStripeWebhook(request);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(casoUsoRegistrarPagoFallido).registrar(eq(idPago), any());
            verify(repositorioEventoProcesado).save(any());
        }
    }

    @Test
    @DisplayName("Evento duplicado: no re-procesa el pago")
    void eventoDuplicado() {
        try (MockedStatic<Webhook> webhook = mockStatic(Webhook.class)) {
            webhook.when(() -> Webhook.constructEvent(any(), any(), any())).thenReturn(evento);
            when(evento.getId()).thenReturn("evt_1");
            when(repositorioEventoProcesado.existsById("evt_1")).thenReturn(true);

            ResponseEntity<String> resp = controlador.handleStripeWebhook(request);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            verifyNoInteractions(casoUsoConfirmarPago);
            verify(repositorioEventoProcesado, never()).save(any());
        }
    }
}
