package com.ecommerce.modulos.busqueda.application;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.ErrorResponse;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.core.search.TotalHitsRelation;
import com.ecommerce.modulos.busqueda.domain.DocumentoProducto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServicioBusquedaTest {

    @Mock
    private ElasticsearchClient elasticsearchClient;

    @InjectMocks
    private ServicioBusqueda servicioBusqueda;

    private UUID idTienda;
    private DocumentoProducto documento;

    @BeforeEach
    void setUp() {
        idTienda = UUID.randomUUID();
        documento = new DocumentoProducto(
                UUID.randomUUID(), idTienda, "Zapatilla Running", "Descripcion de la zapatilla",
                "zapatilla-running", new BigDecimal("120.00"), "Calzado"
        );
    }

    // ---------- indexProduct ----------

    @Test
    void debeIndexarDocumentoConIdIndiceYContenidoCorrectos() throws IOException {
        servicioBusqueda.indexProduct(documento);

        ArgumentCaptor<IndexRequest<DocumentoProducto>> captor = ArgumentCaptor.forClass(IndexRequest.class);
        verify(elasticsearchClient).index(captor.capture());

        IndexRequest<DocumentoProducto> request = captor.getValue();
        assertEquals("productos", request.index());
        assertEquals(documento.getId(), request.id());
        assertSame(documento, request.document());
    }

    @Test
    void noDebePropagarExcepcionSiElasticsearchFallaAlIndexar() throws IOException {
        doThrow(new IOException("elasticsearch caido")).when(elasticsearchClient).index(any(IndexRequest.class));

        assertDoesNotThrow(() -> servicioBusqueda.indexProduct(documento));

        verify(elasticsearchClient).index(any(IndexRequest.class));
    }

    // ---------- busqueda ----------

    @Test
    void debeRetornarProductosCuandoElasticsearchEncuentraResultados() throws IOException {
        Hit<DocumentoProducto> hit = Hit.of(h -> h.index("productos").id(documento.getId()).source(documento));
        HitsMetadata<DocumentoProducto> hitsMetadata = HitsMetadata.of(hm -> hm
                .total(t -> t.value(1).relation(TotalHitsRelation.Eq))
                .hits(List.of(hit)));
        SearchResponse<DocumentoProducto> response = SearchResponse.of(s -> s
                .took(1)
                .timedOut(false)
                .shards(sh -> sh.total(1).successful(1).failed(0))
                .hits(hitsMetadata));

        when(elasticsearchClient.search(any(Function.class), eq(DocumentoProducto.class))).thenReturn(response);

        List<DocumentoProducto> resultado = servicioBusqueda.busqueda(idTienda, "zapatilla");

        assertEquals(1, resultado.size());
        assertSame(documento, resultado.get(0));
    }

    @Test
    void debeRetornarListaVaciaCuandoElasticsearchNoEncuentraResultados() throws IOException {
        HitsMetadata<DocumentoProducto> hitsMetadata = HitsMetadata.of(hm -> hm
                .total(t -> t.value(0).relation(TotalHitsRelation.Eq))
                .hits(List.of()));
        SearchResponse<DocumentoProducto> response = SearchResponse.of(s -> s
                .took(1)
                .timedOut(false)
                .shards(sh -> sh.total(1).successful(1).failed(0))
                .hits(hitsMetadata));

        when(elasticsearchClient.search(any(Function.class), eq(DocumentoProducto.class))).thenReturn(response);

        List<DocumentoProducto> resultado = servicioBusqueda.busqueda(idTienda, "inexistente");

        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
    }

    @Test
    void debeRetornarListaVaciaSinPropagarExcepcionSiElasticsearchFallaAlBuscar() throws IOException {
        when(elasticsearchClient.search(any(Function.class), eq(DocumentoProducto.class)))
                .thenThrow(excepcionSimuladaDeElasticsearch());

        List<DocumentoProducto> resultado = assertDoesNotThrow(() -> servicioBusqueda.busqueda(idTienda, "zapatilla"));

        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void debeConstruirQueryConFiltroDeTiendaYTextoDeBusqueda() throws IOException {
        HitsMetadata<DocumentoProducto> hitsMetadata = HitsMetadata.of(hm -> hm
                .total(t -> t.value(0).relation(TotalHitsRelation.Eq))
                .hits(List.of()));
        SearchResponse<DocumentoProducto> response = SearchResponse.of(s -> s
                .took(1)
                .timedOut(false)
                .shards(sh -> sh.total(1).successful(1).failed(0))
                .hits(hitsMetadata));
        when(elasticsearchClient.search(any(Function.class), eq(DocumentoProducto.class))).thenReturn(response);

        servicioBusqueda.busqueda(idTienda, "zapatilla running");

        ArgumentCaptor<Function> captor = ArgumentCaptor.forClass(Function.class);
        verify(elasticsearchClient).search(captor.capture(), eq(DocumentoProducto.class));

        SearchRequest request = SearchRequest.of((Function) captor.getValue());
        assertEquals(List.of("productos"), request.index());

        BoolQuery boolQuery = request.query().bool();
        assertEquals(2, boolQuery.must().size());

        Query filtroTienda = boolQuery.must().get(0);
        assertTrue(filtroTienda.isTerm());
        TermQuery termQuery = filtroTienda.term();
        assertEquals("idTienda.keyword", termQuery.field());
        assertEquals(idTienda.toString(), termQuery.value().stringValue());

        Query filtroTexto = boolQuery.must().get(1);
        assertTrue(filtroTexto.isMultiMatch());
        MultiMatchQuery multiMatchQuery = filtroTexto.multiMatch();
        assertEquals("zapatilla running", multiMatchQuery.query());
        assertEquals(List.of("nombre", "descripcion", "nombreCategoria"), multiMatchQuery.fields());
        assertEquals("AUTO", multiMatchQuery.fuzziness());
    }

    // ---------- deleteProduct ----------

    @Test
    @SuppressWarnings("unchecked")
    void debeEliminarDocumentoConIdEIndiceCorrectos() throws IOException {
        String idProducto = UUID.randomUUID().toString();

        servicioBusqueda.deleteProduct(idTienda, idProducto);

        ArgumentCaptor<Function> captor = ArgumentCaptor.forClass(Function.class);
        verify(elasticsearchClient).delete(captor.capture());

        DeleteRequest request = DeleteRequest.of((Function) captor.getValue());
        assertEquals("productos", request.index());
        assertEquals(idProducto, request.id());
    }

    private static ElasticsearchException excepcionSimuladaDeElasticsearch() {
        ErrorResponse error = ErrorResponse.of(e -> e
                .status(500)
                .error(c -> c.type("test_error").reason("fallo simulado")));
        return new ElasticsearchException("productos", error);
    }

    @Test
    void noDebePropagarExcepcionSiElasticsearchFallaAlEliminar() throws IOException {
        when(elasticsearchClient.delete(any(Function.class))).thenThrow(excepcionSimuladaDeElasticsearch());

        assertDoesNotThrow(() -> servicioBusqueda.deleteProduct(idTienda, "algun-id"));

        verify(elasticsearchClient).delete(any(Function.class));
    }
}
