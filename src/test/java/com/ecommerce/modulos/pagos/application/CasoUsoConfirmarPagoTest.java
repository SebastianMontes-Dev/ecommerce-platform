package com.ecommerce.modulos.pagos.application;

import com.ecommerce.modulos.compartido.domain.ExcepcionEntidadNoEncontrada;
import com.ecommerce.modulos.ordenes.application.ServicioEstadoOrden;
import com.ecommerce.modulos.pagos.domain.EstadoPago;
import com.ecommerce.modulos.pagos.domain.Pago;
import com.ecommerce.modulos.pagos.domain.RepositorioPago;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CasoUsoConfirmarPagoTest {

    @Mock
    private RepositorioPago repositorioPago;
    @Mock
    private ServicioEstadoOrden servicioEstadoOrden;

    @InjectMocks
    private CasoUsoConfirmarPago casoUsoConfirmarPago;

    private UUID idPago;
    private UUID idOrden;
    private Pago pago;

    @BeforeEach
    void setUp() {
        idPago = UUID.randomUUID();
        idOrden = UUID.randomUUID();

        pago = new Pago();
        pago.setId(idPago);
        pago.setIdOrden(idOrden);
        pago.setIdTienda(UUID.randomUUID());
        pago.setEstado(EstadoPago.PENDING);
        pago.setIdExterno("cs_test_ref");
    }

    @Test
    @DisplayName("Pago PENDING -> pago COMPLETED y se delega la transición de la orden a ordenes")
    void confirmaPagoYDelegaLaOrden() {
        when(repositorioPago.findById(idPago)).thenReturn(Optional.of(pago));

        UUID resultado = casoUsoConfirmarPago.confirmarPagoExitoso(idPago, "pi_123");

        assertEquals(idOrden, resultado);
        assertEquals(EstadoPago.COMPLETED, pago.getEstado());
        assertEquals("pi_123", pago.getIdExterno());
        verify(repositorioPago).save(pago);
        verify(servicioEstadoOrden).marcarPagada(idOrden);
    }

    @Test
    @DisplayName("Webhook duplicado: pago ya COMPLETED -> no toca nada, no delega")
    void esIdempotente() {
        pago.setEstado(EstadoPago.COMPLETED);
        when(repositorioPago.findById(idPago)).thenReturn(Optional.of(pago));

        UUID resultado = casoUsoConfirmarPago.confirmarPagoExitoso(idPago, "pi_123");

        assertEquals(idOrden, resultado);
        verify(repositorioPago, never()).save(any());
        verifyNoInteractions(servicioEstadoOrden);
    }

    @Test
    @DisplayName("Pago inexistente -> ExcepcionEntidadNoEncontrada")
    void fallaSiPagoNoExiste() {
        when(repositorioPago.findById(idPago)).thenReturn(Optional.empty());

        assertThrows(ExcepcionEntidadNoEncontrada.class,
                () -> casoUsoConfirmarPago.confirmarPagoExitoso(idPago, "pi_123"));
        verifyNoInteractions(servicioEstadoOrden);
    }
}
