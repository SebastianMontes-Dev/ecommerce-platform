package com.ecommerce.modules.search.infrastructure;

import com.ecommerce.modules.shared.infrastructure.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ecommerce.modules.search.application.SearchService;
import com.ecommerce.modules.search.domain.ProductDocument;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Búsqueda", description = "Búsqueda de productos de texto completo (Full-text search)")
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    @Operation(summary = "Buscar productos")
    public ResponseEntity<Map<String, Object>> searchProducts(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) Double minRating,
            @RequestParam(defaultValue = "relevance") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("Search: q={}, tenant={}, category={}, priceRange=[{}-{}], rating={}", q, TenantContext.getIdTienda(), category, minPrice, maxPrice, minRating);

        List<ProductDocument> results = searchService.search(TenantContext.getIdTienda(), q);

        return ResponseEntity.ok(Map.of(
                "content", results,
                "page", page,
                "size", size,
                "totalElements", results.size(),
                "totalPages", 1,
                "query", q != null ? q : "",
                "filters", Map.of(
                        "category", category != null ? category : "",
                        "minPrice", minPrice != null ? minPrice : "",
                        "maxPrice", maxPrice != null ? maxPrice : "",
                        "minRating", minRating != null ? minRating : ""
                )
        ));
    }

    @PostMapping("/reindex")
    @Operation(summary = "Reindexar todos los productos (Administrador)")
    public ResponseEntity<Map<String, String>> reindex() {
        log.info("Reindexación iniciada para el tenant: {}", TenantContext.getIdTienda());
        return ResponseEntity.accepted().body(Map.of("message", "Reindexación programada"));
    }
}
