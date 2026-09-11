package com.ecommerce.modulos.busqueda.application;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.ErrorResponse;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.NumberRangeQuery;
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
    void debePropagarExcepcionSiElasticsearchFallaAlIndexar() throws IOException {
        doThrow(new IOException("elasticsearch caido")).when(elasticsearchClient).index(any(IndexRequest.class));

        // El worker del outbox necesita el error para reintentar.
        assertThrows(RuntimeException.class, () -> servicioBusqueda.indexProduct(documento));

        verify(elasticsearchClient).index(any(IndexRequest.class));
    }

    // ---------- busqueda ----------

    private void mockearRespuesta(List<Hit<DocumentoProducto>> hits, long total) throws IOException {
        HitsMetadata<DocumentoProducto> hitsMetadata = HitsMetadata.of(hm -> hm
                .total(t -> t.value((int) total).relation(TotalHitsRelation.Eq))
                .hits(hits));
        SearchResponse<DocumentoProducto> response = SearchResponse.of(s -> s
                .took(1)
                .timedOut(false)
                .shards(sh -> sh.total(1).successful(1).failed(0))
                .hits(hitsMetadata));
        when(elasticsearchClient.search(any(Function.class), eq(DocumentoProducto.class))).thenReturn(response);
    }

    @SuppressWarnings("unchecked")
    private SearchRequest capturarSearchRequest() throws IOException {
        ArgumentCaptor<Function> captor = ArgumentCaptor.forClass(Function.class);
        verify(elasticsearchClient).search(captor.capture(), eq(DocumentoProducto.class));
        return SearchRequest.of((Function) captor.getValue());
    }

    @Test
    void debeRetornarProductosCuandoElasticsearchEncuentraResultados() throws IOException {
        Hit<DocumentoProducto> hit = Hit.of(h -> h.index("productos").id(documento.getId()).source(documento));
        mockearRespuesta(List.of(hit), 1);

        ResultadoBusqueda resultado = servicioBusqueda.busqueda(
                idTienda, "zapatilla", null, null, null, null, "relevance", 0, 20);

        assertEquals(1, resultado.content().size());
        assertSame(documento, resultado.content().get(0));
        assertEquals(1, resultado.totalElements());
    }

    @Test
    void debeRetornarListaVaciaCuandoElasticsearchNoEncuentraResultados() throws IOException {
        mockearRespuesta(List.of(), 0);

        ResultadoBusqueda resultado = servicioBusqueda.busqueda(
                idTienda, "inexistente", null, null, null, null, "relevance", 0, 20);

        assertNotNull(resultado.content());
        assertTrue(resultado.content().isEmpty());
        assertEquals(0, resultado.totalElements());
    }

    @Test
    void debeRetornarResultadoVacioSinPropagarExcepcionSiElasticsearchFallaAlBuscar() throws IOException {
        when(elasticsearchClient.search(any(Function.class), eq(DocumentoProducto.class)))
                .thenThrow(excepcionSimuladaDeElasticsearch());

        ResultadoBusqueda resultado = assertDoesNotThrow(() -> servicioBusqueda.busqueda(
                idTienda, "zapatilla", null, null, null, null, "relevance", 0, 20));

        assertNotNull(resultado.content());
        assertTrue(resultado.content().isEmpty());
        assertEquals(0, resultado.totalElements());
    }

    @Test
    void debeConstruirQueryConFiltroDeTiendaYTextoDeBusqueda() throws IOException {
        mockearRespuesta(List.of(), 0);

        servicioBusqueda.busqueda(idTienda, "zapatilla running", null, null, null, null, "relevance", 0, 20);

        SearchRequest request = capturarSearchRequest();
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

    @Test
    void debeNoAplicarMultiMatchCuandoQueryEsNuloOBlank() throws IOException {
        mockearRespuesta(List.of(), 0);

        servicioBusqueda.busqueda(idTienda, "   ", null, null, null, null, "relevance", 0, 20);

        SearchRequest request = capturarSearchRequest();
        BoolQuery boolQuery = request.query().bool();

        // Solo el filtro obligatorio de idTienda; sin multiMatch cuando query es blank.
        assertEquals(1, boolQuery.must().size());
        assertTrue(boolQuery.must().get(0).isTerm());
    }

    @Test
    void debeAplicarPaginacionRealFromYSize() throws IOException {
        mockearRespuesta(List.of(), 0);

        servicioBusqueda.busqueda(idTienda, null, null, null, null, null, "relevance", 2, 10);

        SearchRequest request = capturarSearchRequest();
        assertEquals(20, request.from());
        assertEquals(10, request.size());
    }

    @Test
    void debeDevolverElTotalRealDeElasticsearchNoElTamanioDeLaListaDeHits() throws IOException {
        Hit<DocumentoProducto> hit = Hit.of(h -> h.index("productos").id(documento.getId()).source(documento));
        mockearRespuesta(List.of(hit), 137);

        ResultadoBusqueda resultado = servicioBusqueda.busqueda(
                idTienda, null, null, null, null, null, "relevance", 0, 20);

        assertEquals(1, resultado.content().size());
        assertEquals(137, resultado.totalElements());
    }

    @Test
    void debeAplicarFiltroDeCategoriaCuandoSeProvee() throws IOException {
        mockearRespuesta(List.of(), 0);

        servicioBusqueda.busqueda(idTienda, null, "Calzado", null, null, null, "relevance", 0, 20);

        SearchRequest request = capturarSearchRequest();
        BoolQuery boolQuery = request.query().bool();

        assertEquals(1, boolQuery.filter().size());
        Query filtroCategoria = boolQuery.filter().get(0);
        assertTrue(filtroCategoria.isTerm());
        TermQuery termQuery = filtroCategoria.term();
        assertEquals("nombreCategoria.keyword", termQuery.field());
        assertEquals("Calzado", termQuery.value().stringValue());
    }

    @Test
    void debeAplicarFiltroDeRangoDePrecioCuandoSeProveenMinYMaxPrice() throws IOException {
        mockearRespuesta(List.of(), 0);

        servicioBusqueda.busqueda(
                idTienda, null, null, new BigDecimal("50"), new BigDecimal("200"), null, "relevance", 0, 20);

        SearchRequest request = capturarSearchRequest();
        BoolQuery boolQuery = request.query().bool();

        assertEquals(1, boolQuery.filter().size());
        Query filtroPrecio = boolQuery.filter().get(0);
        assertTrue(filtroPrecio.isRange());
        assertTrue(filtroPrecio.range().isNumber());
        NumberRangeQuery rangoPrecio = filtroPrecio.range().number();
        assertEquals("precio", rangoPrecio.field());
        assertEquals(50.0, rangoPrecio.gte());
        assertEquals(200.0, rangoPrecio.lte());
    }

    @Test
    void debeAplicarSoloElLimiteInferiorDePrecioCuandoSoloSeProveeMinPrice() throws IOException {
        mockearRespuesta(List.of(), 0);

        servicioBusqueda.busqueda(idTienda, null, null, new BigDecimal("50"), null, null, "relevance", 0, 20);

        SearchRequest request = capturarSearchRequest();
        NumberRangeQuery rangoPrecio = request.query().bool().filter().get(0).range().number();
        assertEquals(50.0, rangoPrecio.gte());
        assertNull(rangoPrecio.lte());
    }

    @Test
    void debeAplicarFiltroDeRatingMinimoCuandoSeProveeMinRating() throws IOException {
        mockearRespuesta(List.of(), 0);

        servicioBusqueda.busqueda(idTienda, null, null, null, null, 4.0, "relevance", 0, 20);

        SearchRequest request = capturarSearchRequest();
        BoolQuery boolQuery = request.query().bool();

        assertEquals(1, boolQuery.filter().size());
        Query filtroRating = boolQuery.filter().get(0);
        assertTrue(filtroRating.isRange());
        NumberRangeQuery rangoRating = filtroRating.range().number();
        assertEquals("calificacionPromedio", rangoRating.field());
        assertEquals(4.0, rangoRating.gte());
    }

    @Test
    void debeOrdenarPorPrecioAscendenteCuandoSortEsPriceAsc() throws IOException {
        mockearRespuesta(List.of(), 0);

        servicioBusqueda.busqueda(idTienda, null, null, null, null, null, "price_asc", 0, 20);

        SearchRequest request = capturarSearchRequest();
        assertEquals(1, request.sort().size());
        assertEquals("precio", request.sort().get(0).field().field());
        assertEquals(SortOrder.Asc, request.sort().get(0).field().order());
    }

    @Test
    void debeOrdenarPorPrecioDescendenteCuandoSortEsPriceDesc() throws IOException {
        mockearRespuesta(List.of(), 0);

        servicioBusqueda.busqueda(idTienda, null, null, null, null, null, "price_desc", 0, 20);

        SearchRequest request = capturarSearchRequest();
        assertEquals(1, request.sort().size());
        assertEquals("precio", request.sort().get(0).field().field());
        assertEquals(SortOrder.Desc, request.sort().get(0).field().order());
    }

    @Test
    void debeOrdenarPorCalificacionCuandoSortEsRating() throws IOException {
        mockearRespuesta(List.of(), 0);

        servicioBusqueda.busqueda(idTienda, null, null, null, null, null, "rating", 0, 20);

        SearchRequest request = capturarSearchRequest();
        assertEquals(1, request.sort().size());
        assertEquals("calificacionPromedio", request.sort().get(0).field().field());
    }

    @Test
    void debeDejarOrdenPorRelevanciaCuandoSortEsRelevanceOValorDesconocido() throws IOException {
        mockearRespuesta(List.of(), 0);

        servicioBusqueda.busqueda(idTienda, null, null, null, null, null, "relevance", 0, 20);

        SearchRequest request = capturarSearchRequest();
        assertTrue(request.sort() == null || request.sort().isEmpty());
    }

    @Test
    void debeIgnorarFiltrosOpcionalesCuandoNoSeProveen() throws IOException {
        mockearRespuesta(List.of(), 0);

        ResultadoBusqueda resultado = assertDoesNotThrow(() -> servicioBusqueda.busqueda(
                idTienda, null, null, null, null, null, "relevance", 0, 20));

        assertNotNull(resultado);
        assertTrue(resultado.content().isEmpty());

        SearchRequest request = capturarSearchRequest();
        BoolQuery boolQuery = request.query().bool();
        assertEquals(1, boolQuery.must().size());
        assertTrue(boolQuery.filter() == null || boolQuery.filter().isEmpty());
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
    void debePropagarExcepcionSiElasticsearchFallaAlEliminar() throws IOException {
        when(elasticsearchClient.delete(any(Function.class))).thenThrow(excepcionSimuladaDeElasticsearch());

        assertThrows(RuntimeException.class, () -> servicioBusqueda.deleteProduct(idTienda, "algun-id"));

        verify(elasticsearchClient).delete(any(Function.class));
    }
}
