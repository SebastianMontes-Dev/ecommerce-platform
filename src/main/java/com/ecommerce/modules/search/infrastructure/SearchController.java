package com.ecommerce.modules.search.infrastructure;

import com.ecommerce.modules.shared.infrastructure.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Search", description = "Full-text product search")
public class SearchController {

    @GetMapping
    @Operation(summary = "Search products")
    public ResponseEntity<Map<String, Object>> searchProducts(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) Double minRating,
            @RequestParam(defaultValue = "relevance") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("Search: q={}, tenant={}, category={}, priceRange=[{}-{}], rating={}", q, TenantContext.getTenantId(), category, minPrice, maxPrice, minRating);

        return ResponseEntity.ok(Map.of(
                "content", List.of(),
                "page", page,
                "size", size,
                "totalElements", 0,
                "totalPages", 0,
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
    @Operation(summary = "Reindex all products (admin)")
    public ResponseEntity<Map<String, String>> reindex() {
        log.info("Reindex triggered for tenant: {}", TenantContext.getTenantId());
        return ResponseEntity.accepted().body(Map.of("message", "Reindex scheduled"));
    }
}
