package com.ecommerce.modulos.compartido.infrastructure.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RegistroOutboxTest {

    @Mock private RepositorioEventoOutbox repositorio;

    private RegistroOutbox registroOutbox;

    @BeforeEach
    void setUp() {
        registroOutbox = new RegistroOutbox(repositorio, new ObjectMapper());
    }

    @Test
    void guardaLaFilaConElPayloadSerializadoAJson() {
        UUID agregado = UUID.randomUUID();
        UUID tienda = UUID.randomUUID();

        registroOutbox.registrar("BUSQUEDA_INDEXAR_PRODUCTO", agregado, tienda, Map.of("nombre", "Camiseta"));

        ArgumentCaptor<EventoOutbox> captor = ArgumentCaptor.forClass(EventoOutbox.class);
        verify(repositorio).save(captor.capture());
        EventoOutbox e = captor.getValue();
        assertEquals("BUSQUEDA_INDEXAR_PRODUCTO", e.getTipo());
        assertEquals(agregado, e.getAgregadoId());
        assertEquals(tienda, e.getIdTienda());
        assertEquals(EstadoEventoOutbox.PENDIENTE, e.getEstado());
        assertTrue(e.getPayload().contains("\"nombre\":\"Camiseta\""));
    }

    @Test
    void lanzaSiElPayloadNoEsSerializable() {
        assertThrows(IllegalArgumentException.class,
                () -> registroOutbox.registrar("X", UUID.randomUUID(), UUID.randomUUID(), new Object()));
    }
}
