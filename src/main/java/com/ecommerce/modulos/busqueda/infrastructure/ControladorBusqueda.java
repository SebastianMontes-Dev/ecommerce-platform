package com.ecommerce.modulos.busqueda.infrastructure;

import com.ecommerce.modulos.compartido.infrastructure.ContextoInquilino;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.ecommerce.modulos.busqueda.application.ResultadoBusqueda;
import com.ecommerce.modulos.busqueda.application.ServicioBusqueda;
import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/busqueda")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Búsqueda", description = "Búsqueda de productos de texto completo (Full-text busqueda)")
public class ControladorBusqueda {

    private final ServicioBusqueda servicioBusqueda;

    @GetMapping
    @Operation(summary = "Buscar productos")
    public ResponseEntity<Map<String, Object>> searchProducts(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Double minRating,
            @RequestParam(defaultValue = "relevance") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("Search: q={}, inquilino={}, categoria={}, priceRange=[{}-{}], calificacion={}", q, ContextoInquilino.getIdTienda(), categoria, minPrice, maxPrice, minRating);

        ResultadoBusqueda resultado = servicioBusqueda.busqueda(
                ContextoInquilino.getIdTienda(), q, categoria, minPrice, maxPrice, minRating, sort, page, size);

        int totalPages = (int) Math.ceil((double) resultado.totalElements() / size);

        return ResponseEntity.ok(Map.of(
                "content", resultado.content(),
                "page", page,
                "size", size,
                "totalElements", resultado.totalElements(),
                "totalPages", totalPages,
                "query", q != null ? q : "",
                "filters", Map.of(
                        "categoria", categoria != null ? categoria : "",
                        "minPrice", minPrice != null ? minPrice : "",
                        "maxPrice", maxPrice != null ? maxPrice : "",
                        "minRating", minRating != null ? minRating : ""
                )
        ));
    }

    @PostMapping("/reindex")
    @Operation(summary = "Reindexar todos los productos (Administrador)")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<Map<String, String>> reindex() {
        log.info("Reindexación iniciada para el inquilino: {}", ContextoInquilino.getIdTienda());
        return ResponseEntity.accepted().body(Map.of("message", "Reindexación programada"));
    }
}
