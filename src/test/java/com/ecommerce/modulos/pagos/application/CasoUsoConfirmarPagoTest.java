package com.ecommerce.modulos.pagos.application;

import com.ecommerce.modulos.compartido.domain.ExcepcionEntidadNoEncontrada;
import com.ecommerce.modulos.ordenes.domain.EstadoOrden;
import com.ecommerce.modulos.ordenes.domain.Orden;
import com.ecommerce.modulos.ordenes.domain.RepositorioOrden;
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
    private RepositorioOrden repositorioOrden;

    @InjectMocks
    private CasoUsoConfirmarPago casoUsoConfirmarPago;

    private UUID idPago;
    private UUID idOrden;
    private Pago pago;
    private Orden orden;

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

        orden = new Orden();
        orden.setId(idOrden);
        orden.setIdTienda(pago.getIdTienda());
        orden.setIdCliente(UUID.randomUUID());
        orden.setNumeroOrden("ORD-1");
        orden.setEstado(EstadoOrden.PENDING);
    }

    @Test
    @DisplayName("Pago PENDING + orden PENDING -> pago COMPLETED, orden PAID, devuelve id de orden")
    void confirmaPagoYOrdenAtomicamente() {
        when(repositorioPago.findById(idPago)).thenReturn(Optional.of(pago));
        when(repositorioOrden.findById(idOrden)).thenReturn(Optional.of(orden));

        UUID resultado = casoUsoConfirmarPago.confirmarPagoExitoso(idPago, "pi_123");

        assertEquals(idOrden, resultado);
        assertEquals(EstadoPago.COMPLETED, pago.getEstado());
        assertEquals("pi_123", pago.getIdExterno());
        assertEquals(EstadoOrden.PAID, orden.getEstado());
        verify(repositorioPago).save(pago);
        verify(repositorioOrden).save(orden);
    }

    @Test
    @DisplayName("Webhook duplicado: pago ya COMPLETED -> no toca nada y devuelve id de orden")
    void esIdempotente() {
        pago.setEstado(EstadoPago.COMPLETED);
        when(repositorioPago.findById(idPago)).thenReturn(Optional.of(pago));

        UUID resultado = casoUsoConfirmarPago.confirmarPagoExitoso(idPago, "pi_123");

        assertEquals(idOrden, resultado);
        verify(repositorioPago, never()).save(any());
        verify(repositorioOrden, never()).findById(any());
        verify(repositorioOrden, never()).save(any());
    }

    @Test
    @DisplayName("Orden ya PAID (carrera con otra confirmación) -> no re-transiciona la orden")
    void noRetransicionaOrdenYaPagada() {
        orden.setEstado(EstadoOrden.PAID);
        when(repositorioPago.findById(idPago)).thenReturn(Optional.of(pago));
        when(repositorioOrden.findById(idOrden)).thenReturn(Optional.of(orden));

        casoUsoConfirmarPago.confirmarPagoExitoso(idPago, "pi_123");

        assertEquals(EstadoPago.COMPLETED, pago.getEstado());
        verify(repositorioOrden, never()).save(any());
    }

    @Test
    @DisplayName("Pago inexistente -> ExcepcionEntidadNoEncontrada")
    void fallaSiPagoNoExiste() {
        when(repositorioPago.findById(idPago)).thenReturn(Optional.empty());

        assertThrows(ExcepcionEntidadNoEncontrada.class,
                () -> casoUsoConfirmarPago.confirmarPagoExitoso(idPago, "pi_123"));
    }

    @Test
    @DisplayName("Orden inexistente -> ExcepcionEntidadNoEncontrada (y la transacción revierte el pago)")
    void fallaSiOrdenNoExiste() {
        when(repositorioPago.findById(idPago)).thenReturn(Optional.of(pago));
        when(repositorioOrden.findById(idOrden)).thenReturn(Optional.empty());

        assertThrows(ExcepcionEntidadNoEncontrada.class,
                () -> casoUsoConfirmarPago.confirmarPagoExitoso(idPago, "pi_123"));
    }
}
