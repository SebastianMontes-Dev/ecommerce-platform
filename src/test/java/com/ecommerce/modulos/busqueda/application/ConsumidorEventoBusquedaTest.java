package com.ecommerce.modulos.busqueda.application;

import com.ecommerce.modulos.busqueda.domain.DocumentoProducto;
import com.ecommerce.modulos.catalogo.domain.eventos.EventoProductoActualizado;
import com.ecommerce.modulos.catalogo.domain.eventos.EventoProductoCreado;
import com.ecommerce.modulos.catalogo.domain.eventos.EventoProductoEliminado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ConsumidorEventoBusquedaTest {

    @Mock
    private ServicioBusqueda servicioBusqueda;

    @InjectMocks
    private ConsumidorEventoBusqueda consumidorEventoBusqueda;

    private UUID idProducto;
    private UUID idTienda;

    @BeforeEach
    void setUp() {
        idProducto = UUID.randomUUID();
        idTienda = UUID.randomUUID();
    }

    @Test
    void debeIndexarDocumentoConDatosDelEventoCuandoSeCreaProducto() {
        EventoProductoCreado evento = new EventoProductoCreado(
                idProducto, idTienda, "Campera", "campera-invierno", "Campera de invierno",
                "ACTIVO", new BigDecimal("99.90"), "Ropa"
        );

        consumidorEventoBusqueda.onProductCreated(evento);

        ArgumentCaptor<DocumentoProducto> captor = ArgumentCaptor.forClass(DocumentoProducto.class);
        verify(servicioBusqueda).indexProduct(captor.capture());

        DocumentoProducto documento = captor.getValue();
        assertEquals(idProducto.toString(), documento.getId());
        assertEquals(idTienda, documento.getIdTienda());
        assertEquals("Campera", documento.getNombre());
        assertEquals("campera-invierno", documento.getEnlaceCorto());
        assertEquals("Campera de invierno", documento.getDescripcion());
        assertEquals(new BigDecimal("99.90"), documento.getPrecio());
        assertEquals("Ropa", documento.getNombreCategoria());
        verify(servicioBusqueda, never()).deleteProduct(any(), any());
    }

    @Test
    void debeIndexarDocumentoConDatosDelEventoCuandoSeActualizaProducto() {
        EventoProductoActualizado evento = new EventoProductoActualizado(
                idProducto, idTienda, "Campera Actualizada", "campera-invierno-v2", "Descripcion actualizada",
                "ACTIVO", new BigDecimal("120.00"), "Ropa de invierno"
        );

        consumidorEventoBusqueda.onProductUpdated(evento);

        ArgumentCaptor<DocumentoProducto> captor = ArgumentCaptor.forClass(DocumentoProducto.class);
        verify(servicioBusqueda).indexProduct(captor.capture());

        DocumentoProducto documento = captor.getValue();
        assertEquals(idProducto.toString(), documento.getId());
        assertEquals(idTienda, documento.getIdTienda());
        assertEquals("Campera Actualizada", documento.getNombre());
        assertEquals("campera-invierno-v2", documento.getEnlaceCorto());
        assertEquals("Descripcion actualizada", documento.getDescripcion());
        assertEquals(new BigDecimal("120.00"), documento.getPrecio());
        assertEquals("Ropa de invierno", documento.getNombreCategoria());
        verify(servicioBusqueda, never()).deleteProduct(any(), any());
    }

    @Test
    void debeEliminarDocumentoDelIndiceCuandoSeEliminaProducto() {
        EventoProductoEliminado evento = new EventoProductoEliminado(idProducto, idTienda);

        consumidorEventoBusqueda.onProductDeleted(evento);

        verify(servicioBusqueda).deleteProduct(idTienda, idProducto.toString());
        verify(servicioBusqueda, never()).indexProduct(any());
    }
}
