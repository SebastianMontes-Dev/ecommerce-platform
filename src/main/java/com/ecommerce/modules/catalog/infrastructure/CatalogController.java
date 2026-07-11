package com.ecommerce.modules.catalog.infrastructure;

import com.ecommerce.modules.catalog.application.*;
import com.ecommerce.modules.catalog.application.dto.*;
import com.ecommerce.modules.shared.infrastructure.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/catalog")
@RequiredArgsConstructor
@Tag(name = "Catalog", description = "Product catalog management")
public class CatalogController {

    private final CreateProductUseCase createProductUseCase;
    private final GetProductUseCase getProductUseCase;
    private final CreateCategoryUseCase createCategoryUseCase;

    @PostMapping("/categories")
    @Operation(summary = "Create a category")
    @CacheEvict(value = "categories", key = "#T(com.ecommerce.modules.shared.infrastructure.TenantContext).getTenantId()")
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        CategoryResponse response = createCategoryUseCase.execute(request, TenantContext.getTenantId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/categories")
    @Operation(summary = "List categories")
    @Cacheable(value = "categories", key = "#T(com.ecommerce.modules.shared.infrastructure.TenantContext).getTenantId()")
    public ResponseEntity<List<CategoryResponse>> listCategories() {
        return ResponseEntity.ok(createCategoryUseCase.getCategories(TenantContext.getTenantId()));
    }

    @PostMapping("/products")
    @Operation(summary = "Create a product")
    @CacheEvict(value = {"products", "product_details"}, allEntries = true) // Clear all product cache for now
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
        ProductResponse response = createProductUseCase.execute(request, TenantContext.getTenantId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/products")
    @Operation(summary = "List products")
    @Cacheable(value = "products", key = "#T(com.ecommerce.modules.shared.infrastructure.TenantContext).getTenantId() + '_' + #page + '_' + #size")
    public ResponseEntity<?> listProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(getProductUseCase.listProducts(
                TenantContext.getTenantId(),
                org.springframework.data.domain.PageRequest.of(page, size)));
    }

    @GetMapping("/products/{slug}")
    @Operation(summary = "Get product by slug")
    @Cacheable(value = "product_details", key = "#T(com.ecommerce.modules.shared.infrastructure.TenantContext).getTenantId() + '_' + #slug")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable String slug) {
        return ResponseEntity.ok(getProductUseCase.bySlug(slug, TenantContext.getTenantId()));
    }
}
