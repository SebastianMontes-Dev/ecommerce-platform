package com.ecommerce.modulos.busqueda.application;

import com.ecommerce.modulos.busqueda.domain.DocumentoProducto;
import com.ecommerce.modulos.catalogo.domain.eventos.EventoProductoCreado;
import com.ecommerce.modulos.catalogo.domain.eventos.EventoProductoEliminado;
import com.ecommerce.modulos.compartido.infrastructure.outbox.RegistroOutbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PuenteOutboxIndexacionTest {

    @Mock private RegistroOutbox registroOutbox;
    @InjectMocks private PuenteOutboxIndexacion puente;

    @Test
    void productoCreadoEncolaUnaIndexacionConElDocumento() {
        UUID id = UUID.randomUUID();
        UUID tienda = UUID.randomUUID();
        var evento = new EventoProductoCreado(id, tienda, "Camiseta", "camiseta", "Algodón",
                "ACTIVE", new BigDecimal("19.99"), "Ropa");

        puente.onProductoCreado(evento);

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(registroOutbox).registrar(
                eq(PuenteOutboxIndexacion.TIPO_INDEXAR), eq(id), eq(tienda), payloadCaptor.capture());
        assertInstanceOf(DocumentoProducto.class, payloadCaptor.getValue());
        assertEquals("Camiseta", ((DocumentoProducto) payloadCaptor.getValue()).getNombre());
    }

    @Test
    void productoEliminadoEncolaUnaEliminacion() {
        UUID id = UUID.randomUUID();
        UUID tienda = UUID.randomUUID();

        puente.onProductoEliminado(new EventoProductoEliminado(id, tienda));

        verify(registroOutbox).registrar(eq(PuenteOutboxIndexacion.TIPO_ELIMINAR), eq(id), eq(tienda), eq(Map.of()));
    }
}
