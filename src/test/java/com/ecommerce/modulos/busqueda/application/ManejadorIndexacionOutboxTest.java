package com.ecommerce.modulos.busqueda.application;

import com.ecommerce.modulos.busqueda.domain.DocumentoProducto;
import com.ecommerce.modulos.compartido.infrastructure.outbox.EventoOutbox;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManejadorIndexacionOutboxTest {

    @Mock private ServicioBusqueda servicioBusqueda;

    private ManejadorIndexacionOutbox manejador;

    @BeforeEach
    void setUp() {
        manejador = new ManejadorIndexacionOutbox(servicioBusqueda, new ObjectMapper());
    }

    @Test
    void soportaLosTiposDeBusqueda() {
        assertTrue(manejador.soporta(PuenteOutboxIndexacion.TIPO_INDEXAR));
        assertTrue(manejador.soporta(PuenteOutboxIndexacion.TIPO_ELIMINAR));
        assertFalse(manejador.soporta("OTRA_COSA"));
    }

    @Test
    void indexaDeserializandoElDocumentoDelPayload() throws Exception {
        UUID id = UUID.randomUUID();
        UUID tienda = UUID.randomUUID();
        String payload = new ObjectMapper().writeValueAsString(
                new DocumentoProducto(id, tienda, "Camiseta", "Camiseta de algodón", "camiseta", new BigDecimal("19.99"), "Ropa"));
        EventoOutbox evento = new EventoOutbox(PuenteOutboxIndexacion.TIPO_INDEXAR, id, tienda, payload);

        manejador.procesar(evento);

        ArgumentCaptor<DocumentoProducto> captor = ArgumentCaptor.forClass(DocumentoProducto.class);
        verify(servicioBusqueda).indexProduct(captor.capture());
        assertEquals("Camiseta", captor.getValue().getNombre());
        assertEquals(id.toString(), captor.getValue().getId());
    }

    @Test
    void eliminaUsandoElAgregadoIdDeLaFila() throws Exception {
        UUID id = UUID.randomUUID();
        UUID tienda = UUID.randomUUID();
        EventoOutbox evento = new EventoOutbox(PuenteOutboxIndexacion.TIPO_ELIMINAR, id, tienda, "{}");

        manejador.procesar(evento);

        verify(servicioBusqueda).deleteProduct(tienda, id.toString());
        verify(servicioBusqueda, never()).indexProduct(any());
    }

    @Test
    void propagaSiElasticsearchFalla() {
        UUID id = UUID.randomUUID();
        EventoOutbox evento = new EventoOutbox(PuenteOutboxIndexacion.TIPO_ELIMINAR, id, UUID.randomUUID(), "{}");
        doThrow(new RuntimeException("ES caído")).when(servicioBusqueda).deleteProduct(any(), any());

        assertThrows(RuntimeException.class, () -> manejador.procesar(evento));
    }
}
