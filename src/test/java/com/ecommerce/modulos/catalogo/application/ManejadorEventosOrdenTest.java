package com.ecommerce.modulos.catalogo.application;

import com.ecommerce.modulos.catalogo.domain.Producto;
import com.ecommerce.modulos.catalogo.domain.RepositorioProducto;
import com.ecommerce.modulos.catalogo.domain.RepositorioVarianteProducto;
import com.ecommerce.modulos.catalogo.domain.VarianteProducto;
import com.ecommerce.modulos.compartido.domain.ExcepcionEntidadNoEncontrada;
import com.ecommerce.modulos.compartido.domain.ExcepcionStockInsuficiente;
import com.ecommerce.modulos.ordenes.domain.ArticuloOrden;
import com.ecommerce.modulos.ordenes.domain.events.EventoInventarioLiberado;
import com.ecommerce.modulos.ordenes.domain.events.EventoOrdenCreada;
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
class ManejadorEventosOrdenTest {

    @Mock
    private RepositorioProducto repositorioProducto;
    @Mock
    private RepositorioVarianteProducto repositorioVarianteProducto;

    @InjectMocks
    private ManejadorEventosOrden manejadorEventosOrden;

    private UUID idTienda;

    @BeforeEach
    void setUp() {
        idTienda = UUID.randomUUID();
    }

    private ArticuloOrden crearArticulo(UUID idProducto, int cantidad) {
        ArticuloOrden articulo = new ArticuloOrden();
        articulo.setIdProducto(idProducto);
        articulo.setCantidad(cantidad);
        return articulo;
    }

    private ArticuloOrden crearArticuloVariante(UUID idProducto, UUID variantId, int cantidad) {
        ArticuloOrden articulo = crearArticulo(idProducto, cantidad);
        articulo.setVariantId(variantId);
        return articulo;
    }

    private Producto productoConStock(UUID id, int stock) {
        Producto producto = new Producto();
        producto.setId(id);
        producto.setInventario(stock);
        producto.setRastreoInventarioHabilitado(true);
        return producto;
    }

    @Test
    @DisplayName("Reserva bloqueando la fila del producto (findByIdForUpdate) y descuenta stock")
    void reservaProductoConLock() {
        UUID idProducto = UUID.randomUUID();
        Producto producto = productoConStock(idProducto, 10);
        when(repositorioProducto.findByIdForUpdate(idProducto)).thenReturn(Optional.of(producto));
        when(repositorioProducto.save(any(Producto.class))).thenAnswer(i -> i.getArguments()[0]);

        manejadorEventosOrden.handle(new EventoOrdenCreada(
                UUID.randomUUID(), idTienda, UUID.randomUUID(), List.of(crearArticulo(idProducto, 3))));

        assertEquals(7, producto.getInventario());
        verify(repositorioProducto).findByIdForUpdate(idProducto);
        verify(repositorioProducto, never()).findById(any());
    }

    @Test
    @DisplayName("Item con variante: descuenta el stock de la VARIANTE, no el del producto padre")
    void reservaDescuentaLaVariante() {
        UUID idProducto = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        VarianteProducto variante = new VarianteProducto();
        variante.setId(variantId);
        variante.setInventario(5);
        when(repositorioVarianteProducto.findByIdForUpdate(variantId)).thenReturn(Optional.of(variante));
        when(repositorioVarianteProducto.save(any())).thenAnswer(i -> i.getArguments()[0]);

        manejadorEventosOrden.handle(new EventoOrdenCreada(
                UUID.randomUUID(), idTienda, UUID.randomUUID(),
                List.of(crearArticuloVariante(idProducto, variantId, 2))));

        assertEquals(3, variante.getInventario());
        verify(repositorioVarianteProducto).findByIdForUpdate(variantId);
        verifyNoInteractions(repositorioProducto);
    }

    @Test
    @DisplayName("Stock insuficiente -> ExcepcionStockInsuficiente; no se guarda nada")
    void stockInsuficienteLanzaExcepcionDedicada() {
        UUID idProducto = UUID.randomUUID();
        Producto producto = productoConStock(idProducto, 1);
        when(repositorioProducto.findByIdForUpdate(idProducto)).thenReturn(Optional.of(producto));

        EventoOrdenCreada evento = new EventoOrdenCreada(
                UUID.randomUUID(), idTienda, UUID.randomUUID(), List.of(crearArticulo(idProducto, 5)));

        assertThrows(ExcepcionStockInsuficiente.class, () -> manejadorEventosOrden.handle(evento));
        verify(repositorioProducto, never()).save(any());
    }

    @Test
    @DisplayName("Producto inexistente -> ExcepcionEntidadNoEncontrada")
    void productoInexistente() {
        UUID idProducto = UUID.randomUUID();
        when(repositorioProducto.findByIdForUpdate(idProducto)).thenReturn(Optional.empty());

        EventoOrdenCreada evento = new EventoOrdenCreada(
                UUID.randomUUID(), idTienda, UUID.randomUUID(), List.of(crearArticulo(idProducto, 1)));

        assertThrows(ExcepcionEntidadNoEncontrada.class, () -> manejadorEventosOrden.handle(evento));
    }

    @Test
    @DisplayName("Cancelación repone stock (producto y variante), simétrico a la reserva")
    void cancelacionReponeStock() {
        UUID idProducto = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        Producto producto = productoConStock(idProducto, 7);
        VarianteProducto variante = new VarianteProducto();
        variante.setId(variantId);
        variante.setInventario(2);
        when(repositorioProducto.findByIdForUpdate(idProducto)).thenReturn(Optional.of(producto));
        when(repositorioVarianteProducto.findByIdForUpdate(variantId)).thenReturn(Optional.of(variante));
        when(repositorioProducto.save(any())).thenAnswer(i -> i.getArguments()[0]);
        when(repositorioVarianteProducto.save(any())).thenAnswer(i -> i.getArguments()[0]);

        manejadorEventosOrden.handle(new EventoInventarioLiberado(
                UUID.randomUUID(), idTienda, "Pago expirado",
                List.of(crearArticulo(idProducto, 3), crearArticuloVariante(UUID.randomUUID(), variantId, 4))));

        assertEquals(10, producto.getInventario());
        assertEquals(6, variante.getInventario());
    }

    @Test
    @DisplayName("Reserva y reposición son simétricas para el mismo producto")
    void reservaYReposicionSonSimetricas() {
        UUID idProducto = UUID.randomUUID();
        Producto producto = productoConStock(idProducto, 10);
        when(repositorioProducto.findByIdForUpdate(idProducto)).thenReturn(Optional.of(producto));
        when(repositorioProducto.save(any())).thenAnswer(i -> i.getArguments()[0]);

        UUID idOrden = UUID.randomUUID();
        manejadorEventosOrden.handle(new EventoOrdenCreada(
                idOrden, idTienda, UUID.randomUUID(), List.of(crearArticulo(idProducto, 4))));
        assertEquals(6, producto.getInventario());

        manejadorEventosOrden.handle(new EventoInventarioLiberado(
                idOrden, idTienda, "Pago rechazado", List.of(crearArticulo(idProducto, 4))));
        assertEquals(10, producto.getInventario());
    }

    @Test
    @DisplayName("Los items se bloquean ordenados por (idProducto, variantId) para evitar deadlocks")
    void procesaItemsEnOrdenDeterminista() {
        UUID pA = UUID.fromString("00000000-0000-0000-0000-0000000000aa");
        UUID pB = UUID.fromString("00000000-0000-0000-0000-0000000000bb");
        when(repositorioProducto.findByIdForUpdate(any()))
                .thenAnswer(i -> Optional.of(productoConStock(i.getArgument(0), 100)));
        when(repositorioProducto.save(any())).thenAnswer(i -> i.getArguments()[0]);

        // Items en orden B, A -> deben bloquearse en orden A, B
        manejadorEventosOrden.handle(new EventoOrdenCreada(
                UUID.randomUUID(), idTienda, UUID.randomUUID(),
                List.of(crearArticulo(pB, 1), crearArticulo(pA, 1))));

        var enOrden = inOrder(repositorioProducto);
        enOrden.verify(repositorioProducto).findByIdForUpdate(pA);
        enOrden.verify(repositorioProducto).findByIdForUpdate(pB);
    }
}
