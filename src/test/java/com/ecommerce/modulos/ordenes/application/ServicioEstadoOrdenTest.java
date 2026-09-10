package com.ecommerce.modulos.ordenes.application;

import com.ecommerce.modulos.compartido.domain.ExcepcionOperacionInvalida;
import com.ecommerce.modulos.compartido.domain.PublicadorEventoDominio;
import com.ecommerce.modulos.ordenes.domain.EstadoOrden;
import com.ecommerce.modulos.ordenes.domain.Orden;
import com.ecommerce.modulos.ordenes.domain.RepositorioOrden;
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
class ServicioEstadoOrdenTest {

    @Mock private RepositorioOrden repositorioOrden;
    @Mock private PublicadorEventoDominio publicadorEventoDominio;

    @InjectMocks private ServicioEstadoOrden servicioEstadoOrden;

    private UUID idOrden;
    private Orden orden;

    @BeforeEach
    void setUp() {
        idOrden = UUID.randomUUID();
        orden = new Orden();
        orden.setId(idOrden);
        orden.setIdTienda(UUID.randomUUID());
        orden.setIdCliente(UUID.randomUUID());
        orden.setNumeroOrden("ORD-1");
        orden.setEstado(EstadoOrden.PENDING);
    }

    @Test
    @DisplayName("marcarPagada: PENDING -> PAID, guarda y publica eventos")
    void marcarPagada() {
        when(repositorioOrden.findById(idOrden)).thenReturn(Optional.of(orden));

        servicioEstadoOrden.marcarPagada(idOrden);

        assertEquals(EstadoOrden.PAID, orden.getEstado());
        verify(repositorioOrden).save(orden);
        verify(publicadorEventoDominio).publish(anyList());
        assertTrue(orden.getDomainEvents().isEmpty());
    }

    @Test
    @DisplayName("marcarPagada es idempotente: orden ya PAID -> no-op")
    void marcarPagadaIdempotente() {
        orden.setEstado(EstadoOrden.PAID);
        when(repositorioOrden.findById(idOrden)).thenReturn(Optional.of(orden));

        servicioEstadoOrden.marcarPagada(idOrden);

        verify(repositorioOrden, never()).save(any());
        verifyNoInteractions(publicadorEventoDominio);
    }

    @Test
    @DisplayName("cancelarPorFalloDePago: PENDING -> CANCELLED y emite EventoInventarioLiberado")
    void cancelarPorFalloDePago() {
        when(repositorioOrden.findById(idOrden)).thenReturn(Optional.of(orden));

        servicioEstadoOrden.cancelarPorFalloDePago(idOrden, "Sesión expirada");

        assertEquals(EstadoOrden.CANCELLED, orden.getEstado());
        verify(repositorioOrden).save(orden);
        verify(publicadorEventoDominio).publish(anyList());
    }

    @Test
    @DisplayName("cancelarPorFalloDePago: orden ya pagada -> no se cancela")
    void cancelarNoAplicaSiYaPagada() {
        orden.setEstado(EstadoOrden.PAID);
        when(repositorioOrden.findById(idOrden)).thenReturn(Optional.of(orden));

        servicioEstadoOrden.cancelarPorFalloDePago(idOrden, "PaymentIntent rechazado");

        assertEquals(EstadoOrden.PAID, orden.getEstado());
        verify(repositorioOrden, never()).save(any());
        verifyNoInteractions(publicadorEventoDominio);
    }

    @Test
    @DisplayName("reembolsar: DELIVERED -> REFUNDED y emite EventoInventarioLiberado")
    void reembolsar() {
        orden.setEstado(EstadoOrden.DELIVERED);
        when(repositorioOrden.findById(idOrden)).thenReturn(Optional.of(orden));

        servicioEstadoOrden.reembolsar(idOrden, "Producto defectuoso");

        assertEquals(EstadoOrden.REFUNDED, orden.getEstado());
        verify(repositorioOrden).save(orden);
        verify(publicadorEventoDominio).publish(anyList());
        assertTrue(orden.getDomainEvents().isEmpty());
    }

    @Test
    @DisplayName("reembolsar es idempotente: orden ya REFUNDED -> no-op")
    void reembolsarIdempotente() {
        orden.setEstado(EstadoOrden.REFUNDED);
        when(repositorioOrden.findById(idOrden)).thenReturn(Optional.of(orden));

        servicioEstadoOrden.reembolsar(idOrden, "x");

        verify(repositorioOrden, never()).save(any());
        verifyNoInteractions(publicadorEventoDominio);
    }

    @Test
    @DisplayName("reembolsar sobre una orden en estado inválido -> ExcepcionOperacionInvalida")
    void reembolsarTransicionInvalida() {
        orden.setEstado(EstadoOrden.PENDING); // PENDING -> REFUNDED no está permitido
        when(repositorioOrden.findById(idOrden)).thenReturn(Optional.of(orden));

        assertThrows(ExcepcionOperacionInvalida.class,
                () -> servicioEstadoOrden.reembolsar(idOrden, "x"));
    }
}
