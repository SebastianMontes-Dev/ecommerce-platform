package com.ecommerce.modulos.pagos.infrastructure.pasarelas;

import com.ecommerce.modulos.compartido.domain.Dinero;
import com.ecommerce.modulos.compartido.domain.ExcepcionOperacionInvalida;
import com.ecommerce.modulos.ordenes.domain.Orden;
import com.ecommerce.modulos.pagos.domain.ResultadoProcesamientoPago;
import com.stripe.exception.ApiConnectionException;
import com.stripe.exception.CardException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcesadorPagoStripeTest {

    @Mock
    private Session session;

    private ProcesadorPagoStripe procesadorPagoStripe;

    private Orden ordenMoc;

    @BeforeEach
    void setUp() {
        procesadorPagoStripe = new ProcesadorPagoStripe();
        ReflectionTestUtils.setField(procesadorPagoStripe, "stripeApiKey", "sk_test_123");

        ordenMoc = new Orden();
        ordenMoc.setNumeroOrden("ORD-12345");
        ordenMoc.setTotal(Dinero.of(new BigDecimal("150.50"), "USD"));
    }

    @Test
    @DisplayName("obtenerIdentificador() devuelve el identificador fijo STRIPE")
    void debeDevolverIdentificadorStripe() {
        assertThat(procesadorPagoStripe.obtenerIdentificador()).isEqualTo("STRIPE");
    }

    @Test
    @DisplayName("procesar(): crea la sesión de checkout en Stripe y devuelve la URL junto con una referencia propia")
    void debeCrearSesionDeCheckoutCorrectamente() {
        try (MockedStatic<Session> sessionEstatico = mockStatic(Session.class)) {
            sessionEstatico.when(() -> Session.create(any(SessionCreateParams.class))).thenReturn(session);
            when(session.getUrl()).thenReturn("https://checkout.stripe.com/session-abc");

            ResultadoProcesamientoPago resultado = procesadorPagoStripe.procesar(ordenMoc);

            assertThat(resultado).isNotNull();
            assertThat(resultado.checkoutUrl()).isEqualTo("https://checkout.stripe.com/session-abc");
            assertThat(resultado.idExterno()).isNotBlank();
        }
    }

    @Test
    @DisplayName("procesar(): envía a Stripe el monto en centavos y la moneda en minúsculas, con un client reference único")
    void debeEnviarLosParametrosCorrectosAStripe() {
        try (MockedStatic<Session> sessionEstatico = mockStatic(Session.class)) {
            ArgumentCaptor<SessionCreateParams> captor = ArgumentCaptor.forClass(SessionCreateParams.class);
            sessionEstatico.when(() -> Session.create(captor.capture())).thenReturn(session);
            when(session.getUrl()).thenReturn("https://checkout.stripe.com/session-abc");

            ResultadoProcesamientoPago resultado = procesadorPagoStripe.procesar(ordenMoc);

            SessionCreateParams paramsEnviados = captor.getValue();
            SessionCreateParams.LineItem lineItem = paramsEnviados.getLineItems().get(0);

            assertThat(lineItem.getPriceData().getCurrency()).isEqualTo("usd");
            assertThat(lineItem.getPriceData().getUnitAmount()).isEqualTo(15050L);
            assertThat(lineItem.getPriceData().getProductData().getName()).isEqualTo("Orden #ORD-12345");
            assertThat(paramsEnviados.getClientReferenceId()).isEqualTo(resultado.idExterno());
        }
    }

    @Test
    @DisplayName("procesar(): dos llamadas generan referencias externas distintas (no reutiliza el mismo idempotency key)")
    void debeGenerarReferenciasDistintasEnLlamadasSucesivas() {
        try (MockedStatic<Session> sessionEstatico = mockStatic(Session.class)) {
            sessionEstatico.when(() -> Session.create(any(SessionCreateParams.class))).thenReturn(session);
            when(session.getUrl()).thenReturn("https://checkout.stripe.com/session-abc");

            ResultadoProcesamientoPago primero = procesadorPagoStripe.procesar(ordenMoc);
            ResultadoProcesamientoPago segundo = procesadorPagoStripe.procesar(ordenMoc);

            assertThat(primero.idExterno()).isNotEqualTo(segundo.idExterno());
        }
    }

    @Test
    @DisplayName("procesar(): si Stripe rechaza la tarjeta (CardException), se traduce a ExcepcionOperacionInvalida")
    void debeLanzarExcepcionOperacionInvalidaCuandoTarjetaEsRechazada() {
        try (MockedStatic<Session> sessionEstatico = mockStatic(Session.class)) {
            CardException tarjetaRechazada = new CardException(
                    "Your card was declined.", "req_123", "card_declined", null,
                    "generic_decline", null, 402, null);
            sessionEstatico.when(() -> Session.create(any(SessionCreateParams.class))).thenThrow(tarjetaRechazada);

            ExcepcionOperacionInvalida excepcion = assertThrows(ExcepcionOperacionInvalida.class,
                    () -> procesadorPagoStripe.procesar(ordenMoc));

            assertThat(excepcion.getMessage()).isEqualTo("No se pudo iniciar el proceso de pago con Stripe.");
        }
    }

    @Test
    @DisplayName("procesar(): si hay un error de red/API con Stripe (ApiConnectionException), se traduce a ExcepcionOperacionInvalida")
    void debeLanzarExcepcionOperacionInvalidaCuandoHayErrorDeConexion() {
        try (MockedStatic<Session> sessionEstatico = mockStatic(Session.class)) {
            ApiConnectionException errorDeRed = new ApiConnectionException("Timeout al conectar con Stripe");
            sessionEstatico.when(() -> Session.create(any(SessionCreateParams.class))).thenThrow(errorDeRed);

            assertThrows(ExcepcionOperacionInvalida.class, () -> procesadorPagoStripe.procesar(ordenMoc));
        }
    }

    @Test
    @DisplayName("fallbackProcesar(): cuando el circuit breaker está abierto o Stripe falla, siempre lanza ExcepcionOperacionInvalida")
    void debeLanzarExcepcionOperacionInvalidaDesdeElFallback() {
        RuntimeException causaOriginal = new RuntimeException("circuit breaker abierto");

        ExcepcionOperacionInvalida excepcion = assertThrows(ExcepcionOperacionInvalida.class,
                () -> procesadorPagoStripe.fallbackProcesar(ordenMoc, causaOriginal));

        assertThat(excepcion.getMessage())
                .isEqualTo("El proveedor de pagos no está disponible temporalmente. Intente de nuevo más tarde.");
    }
}
