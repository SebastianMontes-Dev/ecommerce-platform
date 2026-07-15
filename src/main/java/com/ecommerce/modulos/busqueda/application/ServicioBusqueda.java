package com.ecommerce.modulos.busqueda.application;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.ecommerce.modulos.busqueda.domain.DocumentoProducto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServicioBusqueda {

    private final ElasticsearchClient elasticsearchClient;
    private static final String INDEX_NAME = "productos";

    public void indexProduct(DocumentoProducto document) {
        try {
            IndexRequest<DocumentoProducto> request = IndexRequest.of(i -> i
                    .index(INDEX_NAME)
                    .id(document.getId())
                    .document(document)
            );
            elasticsearchClient.index(request);
            log.info("Indexed producto {} in Elasticsearch", document.getId());
        } catch (Exception e) {
            log.error("Failed to index producto {}", document.getId(), e);
        }
    }

    public List<DocumentoProducto> busqueda(UUID idTienda, String query) {
        try {
            SearchResponse<DocumentoProducto> response = elasticsearchClient.search(s -> s
                    .index(INDEX_NAME)
                    .query(q -> q
                        .bool(b -> b
                            .must(m -> m.term(t -> t.field("idTienda.keyword").value(idTienda.toString())))
                            .must(m -> m.multiMatch(mm -> mm
                                .fields("nombre", "descripcion", "nombreCategoria")
                                .query(query)
                                .fuzziness("AUTO")
                            ))
                        )
                    ), DocumentoProducto.class);

            return response.hits().hits().stream()
                    .map(hit -> hit.source())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to busqueda productos", e);
            return List.of();
        }
    }
}
