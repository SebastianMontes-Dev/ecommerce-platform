package com.ecommerce.modulos.compartido.infrastructure.outbox;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcesadorOutboxTest {

    @Mock private RepositorioEventoOutbox repositorio;
    @Mock private ManejadorEventoOutbox manejador;
    @Mock private PlatformTransactionManager txManager;

    private ProcesadorOutbox procesador;

    private UUID id;
    private EventoOutbox evento;

    @BeforeEach
    void setUp() {
        procesador = new ProcesadorOutbox(repositorio, List.of(manejador), txManager);
        ReflectionTestUtils.setField(procesador, "maxIntentos", 5);
        ReflectionTestUtils.setField(procesador, "tamanoLote", 50);
        ReflectionTestUtils.setField(procesador, "minutosColgado", 2L);

        id = UUID.randomUUID();
        evento = new EventoOutbox("BUSQUEDA_INDEXAR_PRODUCTO", UUID.randomUUID(), UUID.randomUUID(), "{}");
        ReflectionTestUtils.setField(evento, "id", id);
        evento.marcarEnProceso();
    }

    @Test
    @DisplayName("procesarUno: handler OK -> evento PROCESADO")
    void procesadoOk() throws Exception {
        when(repositorio.findById(id)).thenReturn(Optional.of(evento));
        when(manejador.soporta("BUSQUEDA_INDEXAR_PRODUCTO")).thenReturn(true);

        procesador.procesarUno(id);

        verify(manejador).procesar(evento);
        assertEquals(EstadoEventoOutbox.PROCESADO, evento.getEstado());
        assertNotNull(evento.getProcesadoEn());
    }

    @Test
    @DisplayName("procesarUno: handler falla y quedan reintentos -> vuelve a PENDIENTE con backoff")
    void falloReprogramado() throws Exception {
        when(repositorio.findById(id)).thenReturn(Optional.of(evento));
        when(manejador.soporta(any())).thenReturn(true);
        doThrow(new RuntimeException("ES caído")).when(manejador).procesar(evento);

        procesador.procesarUno(id);

        assertEquals(EstadoEventoOutbox.PENDIENTE, evento.getEstado());
        assertEquals(1, evento.getIntentos());
        assertTrue(evento.getProximoIntentoEn().isAfter(LocalDateTime.now().plusSeconds(1)));
        assertEquals("ES caído", evento.getUltimoError());
    }

    @Test
    @DisplayName("procesarUno: handler falla y se agotan los reintentos -> FALLIDO")
    void falloAgotado() throws Exception {
        ReflectionTestUtils.setField(evento, "intentos", 4); // el 5º fallo es el último
        when(repositorio.findById(id)).thenReturn(Optional.of(evento));
        when(manejador.soporta(any())).thenReturn(true);
        doThrow(new RuntimeException("ES caído")).when(manejador).procesar(evento);

        procesador.procesarUno(id);

        assertEquals(EstadoEventoOutbox.FALLIDO, evento.getEstado());
    }

    @Test
    @DisplayName("procesarUno: sin manejador para el tipo -> se trata como fallo")
    void sinManejador() {
        ReflectionTestUtils.setField(evento, "tipo", "TIPO_DESCONOCIDO");
        when(repositorio.findById(id)).thenReturn(Optional.of(evento));
        when(manejador.soporta("TIPO_DESCONOCIDO")).thenReturn(false);

        procesador.procesarUno(id);

        assertEquals(EstadoEventoOutbox.PENDIENTE, evento.getEstado());
        assertEquals(1, evento.getIntentos());
    }

    @Test
    @DisplayName("procesarUno: evento que ya no está PROCESANDO (lo tomó otro) -> no-op")
    void yaNoEstaEnProceso() throws Exception {
        evento.marcarProcesado();
        when(repositorio.findById(id)).thenReturn(Optional.of(evento));

        procesador.procesarUno(id);

        verifyNoInteractions(manejador);
    }
}
