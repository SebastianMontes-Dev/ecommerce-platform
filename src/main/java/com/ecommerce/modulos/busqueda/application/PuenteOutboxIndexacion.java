package com.ecommerce.modulos.busqueda.application;

import com.ecommerce.modulos.busqueda.domain.DocumentoProducto;
import com.ecommerce.modulos.catalogo.domain.eventos.EventoProductoActualizado;
import com.ecommerce.modulos.catalogo.domain.eventos.EventoProductoCreado;
import com.ecommerce.modulos.catalogo.domain.eventos.EventoProductoEliminado;
import com.ecommerce.modulos.compartido.infrastructure.outbox.RegistroOutbox;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Traduce los eventos de dominio de catálogo en filas de outbox destinadas a la
 * reindexación en Elasticsearch. {@code BEFORE_COMMIT}: la fila de outbox se escribe en la
 * misma transacción que el cambio del producto (o ninguna de las dos). El worker
 * ({@code ProcesadorOutbox}) se encarga de la entrega a ES con reintentos.
 */
@Component
@RequiredArgsConstructor
public class PuenteOutboxIndexacion {

    public static final String TIPO_INDEXAR = "BUSQUEDA_INDEXAR_PRODUCTO";
    public static final String TIPO_ELIMINAR = "BUSQUEDA_ELIMINAR_PRODUCTO";

    private final RegistroOutbox registroOutbox;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onProductoCreado(EventoProductoCreado e) {
        encolarIndexacion(e.getIdProducto(), e.getIdTienda(), e.getNombre(), e.getDescripcion(),
                e.getEnlaceCorto(), e.getPrecio(), e.getNombreCategoria());
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onProductoActualizado(EventoProductoActualizado e) {
        encolarIndexacion(e.getIdProducto(), e.getIdTienda(), e.getNombre(), e.getDescripcion(),
                e.getEnlaceCorto(), e.getPrecio(), e.getNombreCategoria());
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onProductoEliminado(EventoProductoEliminado e) {
        registroOutbox.registrar(TIPO_ELIMINAR, e.getIdProducto(), e.getIdTienda(), Map.of());
    }

    private void encolarIndexacion(UUID idProducto, UUID idTienda, String nombre, String descripcion,
                                   String enlaceCorto, BigDecimal precio, String nombreCategoria) {
        DocumentoProducto doc = new DocumentoProducto(
                idProducto, idTienda, nombre, descripcion, enlaceCorto, precio, nombreCategoria);
        registroOutbox.registrar(TIPO_INDEXAR, idProducto, idTienda, doc);
    }
}
