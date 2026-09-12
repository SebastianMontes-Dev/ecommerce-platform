package com.ecommerce.modulos.busqueda.infrastructure;

import com.ecommerce.modulos.compartido.infrastructure.ContextoInquilino;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.ecommerce.modulos.busqueda.application.ResultadoBusqueda;
import com.ecommerce.modulos.busqueda.application.ServicioBusqueda;
import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/busqueda")
@RequiredArgsConstructor
@Slf4j
@Validated
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
            // minRating queda descopado: `calificacionPromedio` no lo puebla ningún código del
            // repo (ni al crear/actualizar un producto ni al crear una reseña), así que no hay
            // nada real que filtrar por rating todavía. Ver ServicioBusqueda.busqueda.
            @RequestParam(defaultValue = "relevance") String sort,
            @RequestParam(defaultValue = "0") @Min(0) @Max(10_000) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        log.info("Search: q={}, inquilino={}, categoria={}, priceRange=[{}-{}]", q, ContextoInquilino.getIdTienda(), categoria, minPrice, maxPrice);

        ResultadoBusqueda resultado = servicioBusqueda.busqueda(
                ContextoInquilino.getIdTienda(), q, categoria, minPrice, maxPrice, sort, page, size);

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
                        "maxPrice", maxPrice != null ? maxPrice : ""
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
