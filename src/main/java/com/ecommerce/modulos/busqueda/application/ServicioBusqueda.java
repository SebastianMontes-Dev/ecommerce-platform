package com.ecommerce.modulos.busqueda.application;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.ecommerce.modulos.busqueda.domain.DocumentoProducto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServicioBusqueda {

    private final ElasticsearchClient elasticsearchClient;
    private static final String INDEX_NAME = "productos";

    /**
     * Indexa el documento en Elasticsearch. <b>Propaga</b> la excepción si ES falla: el
     * llamador es el worker del outbox, que necesita el error para reintentar. Perder
     * silenciosamente la indexación era lo que dejaba PostgreSQL y ES divergentes.
     */
    public void indexProduct(DocumentoProducto document) {
        try {
            elasticsearchClient.index(IndexRequest.of(i -> i
                    .index(INDEX_NAME)
                    .id(document.getId())
                    .document(document)));
            log.info("Indexed producto {} in Elasticsearch", document.getId());
        } catch (Exception e) {
            log.error("Failed to index producto {}", document.getId(), e);
            throw new RuntimeException("No se pudo indexar el producto " + document.getId() + " en Elasticsearch", e);
        }
    }

    /**
     * Busca productos en Elasticsearch aplicando realmente todos los filtros/paginación/orden
     * que recibe (ver hallazgo Fase 9 / Task 2: antes solo se usaba {@code idTienda} y
     * {@code query}, el resto se ignoraba).
     */
    public ResultadoBusqueda busqueda(UUID idTienda, String query, String categoria, BigDecimal minPrice,
                                       BigDecimal maxPrice, Double minRating, String sort, int page, int size) {
        try {
            SearchResponse<DocumentoProducto> response = elasticsearchClient.search(s -> {
                s.index(INDEX_NAME)
                        .from(page * size)
                        .size(size)
                        .query(q -> q.bool(b -> {
                            b.must(m -> m.term(t -> t.field("idTienda.keyword").value(idTienda.toString())));

                            if (query != null && !query.isBlank()) {
                                b.must(m -> m.multiMatch(mm -> mm
                                        .fields("nombre", "descripcion", "nombreCategoria")
                                        .query(query)
                                        .fuzziness("AUTO")
                                ));
                            }

                            if (categoria != null && !categoria.isBlank()) {
                                b.filter(f -> f.term(t -> t.field("nombreCategoria.keyword").value(categoria)));
                            }

                            if (minPrice != null || maxPrice != null) {
                                b.filter(f -> f.range(r -> r.number(n -> {
                                    n.field("precio");
                                    if (minPrice != null) {
                                        n.gte(minPrice.doubleValue());
                                    }
                                    if (maxPrice != null) {
                                        n.lte(maxPrice.doubleValue());
                                    }
                                    return n;
                                })));
                            }

                            if (minRating != null) {
                                b.filter(f -> f.range(r -> r.number(n -> n
                                        .field("calificacionPromedio")
                                        .gte(minRating))));
                            }

                            return b;
                        }));

                if (sort != null) {
                    switch (sort) {
                        case "price_asc" -> s.sort(so -> so.field(f -> f.field("precio").order(SortOrder.Asc)));
                        case "price_desc" -> s.sort(so -> so.field(f -> f.field("precio").order(SortOrder.Desc)));
                        case "rating" -> s.sort(so -> so.field(f -> f.field("calificacionPromedio").order(SortOrder.Desc)));
                        default -> {
                            // "relevance" (default) y cualquier valor desconocido: se deja el orden
                            // por relevancia de Elasticsearch, no se setea .sort(...).
                        }
                    }
                }

                return s;
            }, DocumentoProducto.class);

            List<DocumentoProducto> content = response.hits().hits().stream()
                    .map(hit -> hit.source())
                    .collect(Collectors.toList());
            long totalElements = response.hits().total() != null ? response.hits().total().value() : content.size();

            return new ResultadoBusqueda(content, totalElements);
        } catch (Exception e) {
            log.error("Failed to busqueda productos", e);
            return new ResultadoBusqueda(List.of(), 0);
        }
    }

    /** Elimina el documento del índice. Propaga la excepción (ver {@link #indexProduct}). */
    public void deleteProduct(UUID idTienda, String idProducto) {
        try {
            elasticsearchClient.delete(d -> d.index(INDEX_NAME).id(idProducto));
            log.info("Deleted producto {} from Elasticsearch", idProducto);
        } catch (Exception e) {
            log.error("Failed to delete producto {}", idProducto, e);
            throw new RuntimeException("No se pudo eliminar el producto " + idProducto + " de Elasticsearch", e);
        }
    }
}
