package com.ecommerce.modulos.notificacion.application;

import com.ecommerce.modulos.ordenes.domain.EstadoOrden;
import com.ecommerce.modulos.ordenes.domain.eventos.EventoEstadoOrdenCambiado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OyenteEventoOrdenTest {

    @Mock
    private ServicioNotificacionCorreo servicioNotificacionCorreo;

    @InjectMocks
    private OyenteEventoOrden oyenteEventoOrden;

    private UUID idOrden;
    private UUID idTienda;

    @BeforeEach
    void setUp() {
        idOrden = UUID.randomUUID();
        idTienda = UUID.randomUUID();
    }

    @Test
    void debeEnviarCorreoDeConfirmacionCuandoEstadoCambiaAConfirmed() {
        EventoEstadoOrdenCambiado evento = new EventoEstadoOrdenCambiado(
                idOrden, idTienda, EstadoOrden.PENDING, EstadoOrden.CONFIRMED, null);

        oyenteEventoOrden.onOrderStatusChanged(evento);

        verify(servicioNotificacionCorreo).sendOrderConfirmation(idOrden, idTienda);
        verifyNoMoreInteractions(servicioNotificacionCorreo);
    }

    @Test
    void debeEnviarCorreoDePagoRecibidoCuandoEstadoCambiaAPaid() {
        EventoEstadoOrdenCambiado evento = new EventoEstadoOrdenCambiado(
                idOrden, idTienda, EstadoOrden.CONFIRMED, EstadoOrden.PAID, null);

        oyenteEventoOrden.onOrderStatusChanged(evento);

        verify(servicioNotificacionCorreo).sendPaymentReceived(idOrden, idTienda);
        verifyNoMoreInteractions(servicioNotificacionCorreo);
    }

    @Test
    void debeEnviarCorreoDeEnvioCuandoEstadoCambiaAShipped() {
        EventoEstadoOrdenCambiado evento = new EventoEstadoOrdenCambiado(
                idOrden, idTienda, EstadoOrden.PAID, EstadoOrden.SHIPPED, null);

        oyenteEventoOrden.onOrderStatusChanged(evento);

        verify(servicioNotificacionCorreo).sendOrderShipped(idOrden, idTienda);
        verifyNoMoreInteractions(servicioNotificacionCorreo);
    }

    @Test
    void debeEnviarCorreoDeEntregaCuandoEstadoCambiaADelivered() {
        EventoEstadoOrdenCambiado evento = new EventoEstadoOrdenCambiado(
                idOrden, idTienda, EstadoOrden.SHIPPED, EstadoOrden.DELIVERED, null);

        oyenteEventoOrden.onOrderStatusChanged(evento);

        verify(servicioNotificacionCorreo).sendOrderDelivered(idOrden, idTienda);
        verifyNoMoreInteractions(servicioNotificacionCorreo);
    }

    @Test
    void debeEnviarCorreoDeCancelacionCuandoEstadoCambiaACancelled() {
        EventoEstadoOrdenCambiado evento = new EventoEstadoOrdenCambiado(
                idOrden, idTienda, EstadoOrden.PENDING, EstadoOrden.CANCELLED, "cliente se arrepintió");

        oyenteEventoOrden.onOrderStatusChanged(evento);

        verify(servicioNotificacionCorreo).sendOrderCancelled(idOrden, idTienda);
        verifyNoMoreInteractions(servicioNotificacionCorreo);
    }

    @Test
    void debeEnviarCorreoDeReembolsoCuandoEstadoCambiaARefunded() {
        EventoEstadoOrdenCambiado evento = new EventoEstadoOrdenCambiado(
                idOrden, idTienda, EstadoOrden.CANCELLED, EstadoOrden.REFUNDED, null);

        oyenteEventoOrden.onOrderStatusChanged(evento);

        verify(servicioNotificacionCorreo).sendOrderRefunded(idOrden, idTienda);
        verifyNoMoreInteractions(servicioNotificacionCorreo);
    }

    @ParameterizedTest
    @EnumSource(value = EstadoOrden.class, names = {"PENDING", "PROCESSING"})
    void noDebeEnviarNingunCorreoParaEstadosSinNotificacionConfigurada(EstadoOrden nuevoEstado) {
        EventoEstadoOrdenCambiado evento = new EventoEstadoOrdenCambiado(
                idOrden, idTienda, EstadoOrden.PENDING, nuevoEstado, null);

        oyenteEventoOrden.onOrderStatusChanged(evento);

        verifyNoInteractions(servicioNotificacionCorreo);
    }

    @Test
    void debePropagarExcepcionSiElServicioDeCorreoFalla() {
        EventoEstadoOrdenCambiado evento = new EventoEstadoOrdenCambiado(
                idOrden, idTienda, EstadoOrden.PENDING, EstadoOrden.CONFIRMED, null);
        doThrow(new RuntimeException("Error en SMTP"))
                .when(servicioNotificacionCorreo).sendOrderConfirmation(idOrden, idTienda);

        // El listener NO tiene try/catch propio: si el envío de correo falla,
        // la excepción se propaga (queda a cargo del manejador de @Async de Spring).
        assertThrows(RuntimeException.class, () -> oyenteEventoOrden.onOrderStatusChanged(evento));
    }
}
