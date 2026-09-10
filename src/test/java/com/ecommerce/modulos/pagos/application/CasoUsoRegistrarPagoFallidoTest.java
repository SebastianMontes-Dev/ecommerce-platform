package com.ecommerce.modulos.pagos.application;

import com.ecommerce.modulos.compartido.domain.ExcepcionEntidadNoEncontrada;
import com.ecommerce.modulos.compartido.domain.PublicadorEventoDominio;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CasoUsoRegistrarPagoFallidoTest {

    @Mock private RepositorioPago repositorioPago;
    @Mock private RepositorioOrden repositorioOrden;
    @Mock private PublicadorEventoDominio publicadorEventoDominio;

    @InjectMocks private CasoUsoRegistrarPagoFallido casoUso;

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

        orden = new Orden();
        orden.setId(idOrden);
        orden.setIdTienda(pago.getIdTienda());
        orden.setIdCliente(UUID.randomUUID());
        orden.setNumeroOrden("ORD-1");
        orden.setEstado(EstadoOrden.PENDING);
    }

    @Test
    @DisplayName("Pago PENDING + orden PENDING -> pago FAILED, orden CANCELLED y se publican eventos")
    void marcaFallidoYCancelaOrden() {
        when(repositorioPago.findById(idPago)).thenReturn(Optional.of(pago));
        when(repositorioOrden.findById(idOrden)).thenReturn(Optional.of(orden));

        casoUso.registrar(idPago, "Sesión expirada");

        assertEquals(EstadoPago.FAILED, pago.getEstado());
        assertEquals(EstadoOrden.CANCELLED, orden.getEstado());
        verify(repositorioPago).save(pago);
        verify(repositorioOrden).save(orden);
        verify(publicadorEventoDominio).publish(anyList());
    }

    @Test
    @DisplayName("Tras publicar, los eventos de dominio de la orden quedan limpios")
    void limpiaEventosTrasPublicar() {
        when(repositorioPago.findById(idPago)).thenReturn(Optional.of(pago));
        when(repositorioOrden.findById(idOrden)).thenReturn(Optional.of(orden));

        casoUso.registrar(idPago, "Sesión expirada");

        assertTrue(orden.getDomainEvents().isEmpty());
        verify(publicadorEventoDominio).publish(anyList());
    }

    @Test
    @DisplayName("Idempotente: pago ya FAILED -> no hace nada")
    void idempotenteSiYaFallido() {
        pago.setEstado(EstadoPago.FAILED);
        when(repositorioPago.findById(idPago)).thenReturn(Optional.of(pago));

        casoUso.registrar(idPago, "Sesión expirada");

        verify(repositorioPago, never()).save(any());
        verifyNoInteractions(repositorioOrden, publicadorEventoDominio);
    }

    @Test
    @DisplayName("Pago ya COMPLETED (evento fuera de orden) -> no se toca la orden")
    void noCancelaSiPagoYaCompletado() {
        pago.setEstado(EstadoPago.COMPLETED);
        when(repositorioPago.findById(idPago)).thenReturn(Optional.of(pago));

        casoUso.registrar(idPago, "PaymentIntent rechazado");

        verify(repositorioPago, never()).save(any());
        verifyNoInteractions(repositorioOrden, publicadorEventoDominio);
    }

    @Test
    @DisplayName("Orden ya pagada -> pago pasa a FAILED pero la orden NO se cancela")
    void noCancelaOrdenYaPagada() {
        orden.setEstado(EstadoOrden.PAID);
        when(repositorioPago.findById(idPago)).thenReturn(Optional.of(pago));
        when(repositorioOrden.findById(idOrden)).thenReturn(Optional.of(orden));

        casoUso.registrar(idPago, "PaymentIntent rechazado");

        assertEquals(EstadoPago.FAILED, pago.getEstado());
        assertEquals(EstadoOrden.PAID, orden.getEstado());
        verify(repositorioOrden, never()).save(any());
        verifyNoInteractions(publicadorEventoDominio);
    }

    @Test
    @DisplayName("Pago inexistente -> ExcepcionEntidadNoEncontrada")
    void fallaSiPagoNoExiste() {
        when(repositorioPago.findById(idPago)).thenReturn(Optional.empty());

        assertThrows(ExcepcionEntidadNoEncontrada.class, () -> casoUso.registrar(idPago, "x"));
    }
}
