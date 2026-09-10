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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CasoUsoRegistrarPagoFallidoTest {

    @Mock private RepositorioPago repositorioPago;
    @Mock private ServicioEstadoOrden servicioEstadoOrden;

    @InjectMocks private CasoUsoRegistrarPagoFallido casoUso;

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
    }

    @Test
    @DisplayName("Pago PENDING -> pago FAILED y se delega la cancelación de la orden a ordenes")
    void marcaFallidoYDelegaCancelacion() {
        when(repositorioPago.findById(idPago)).thenReturn(Optional.of(pago));

        casoUso.registrar(idPago, "Sesión expirada");

        assertEquals(EstadoPago.FAILED, pago.getEstado());
        verify(repositorioPago).save(pago);
        verify(servicioEstadoOrden).cancelarPorFalloDePago(eq(idOrden), any());
    }

    @Test
    @DisplayName("Idempotente: pago ya FAILED -> no hace nada")
    void idempotenteSiYaFallido() {
        pago.setEstado(EstadoPago.FAILED);
        when(repositorioPago.findById(idPago)).thenReturn(Optional.of(pago));

        casoUso.registrar(idPago, "Sesión expirada");

        verify(repositorioPago, never()).save(any());
        verifyNoInteractions(servicioEstadoOrden);
    }

    @Test
    @DisplayName("Pago ya COMPLETED (evento fuera de orden) -> no se toca la orden")
    void noCancelaSiPagoYaCompletado() {
        pago.setEstado(EstadoPago.COMPLETED);
        when(repositorioPago.findById(idPago)).thenReturn(Optional.of(pago));

        casoUso.registrar(idPago, "PaymentIntent rechazado");

        verify(repositorioPago, never()).save(any());
        verifyNoInteractions(servicioEstadoOrden);
    }

    @Test
    @DisplayName("Pago inexistente -> ExcepcionEntidadNoEncontrada")
    void fallaSiPagoNoExiste() {
        when(repositorioPago.findById(idPago)).thenReturn(Optional.empty());

        assertThrows(ExcepcionEntidadNoEncontrada.class, () -> casoUso.registrar(idPago, "x"));
    }
}
