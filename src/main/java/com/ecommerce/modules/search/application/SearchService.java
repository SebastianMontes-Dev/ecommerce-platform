package com.ecommerce.modules.search.application;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.ecommerce.modules.search.domain.ProductDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final ElasticsearchClient elasticsearchClient;
    private static final String INDEX_NAME = "products";

    public void indexProduct(ProductDocument document) {
        try {
            IndexRequest<ProductDocument> request = IndexRequest.of(i -> i
                    .index(INDEX_NAME)
                    .id(document.getId())
                    .document(document)
            );
            elasticsearchClient.index(request);
            log.info("Indexed product {} in Elasticsearch", document.getId());
        } catch (Exception e) {
            log.error("Failed to index product {}", document.getId(), e);
        }
    }

    public List<ProductDocument> search(UUID tenantId, String query) {
        try {
            SearchResponse<ProductDocument> response = elasticsearchClient.search(s -> s
                    .index(INDEX_NAME)
                    .query(q -> q
                        .bool(b -> b
                            .must(m -> m.term(t -> t.field("tenantId.keyword").value(tenantId.toString())))
                            .must(m -> m.multiMatch(mm -> mm
                                .fields("name", "description", "categoryName")
                                .query(query)
                                .fuzziness("AUTO")
                            ))
                        )
                    ), ProductDocument.class);

            return response.hits().hits().stream()
                    .map(hit -> hit.source())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to search products", e);
            return List.of();
        }
    }
}
