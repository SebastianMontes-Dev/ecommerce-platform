package com.ecommerce.modulos.catalogo.infrastructure;

import com.ecommerce.modulos.catalogo.application.*;
import com.ecommerce.modulos.catalogo.application.dto.*;
import com.ecommerce.modulos.compartido.infrastructure.ContextoInquilino;
import com.ecommerce.modulos.compartido.infrastructure.RespuestaPaginada;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/catalogo")
@RequiredArgsConstructor
@Tag(name = "Catálogo", description = "Gestión del catálogo de productos")
public class ControladorCatalogo {

    private final CasoUsoCrearProducto casoUsoCrearProducto;
    private final CasoUsoObtenerProducto casoUsoObtenerProducto;
    private final CasoUsoCrearCategoria casoUsoCrearCategoria;

    @PostMapping("/categorias")
    @Operation(summary = "Crear una categoría")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<RespuestaCategoria> createCategory(@Valid @RequestBody SolicitudCrearCategoria request) {
        RespuestaCategoria response = casoUsoCrearCategoria.execute(request, ContextoInquilino.getIdTiendaPropia());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/categorias")
    @Operation(summary = "Listar categorías")
    public ResponseEntity<List<RespuestaCategoria>> listCategories() {
        return ResponseEntity.ok(casoUsoCrearCategoria.getCategories(ContextoInquilino.getIdTienda()));
    }

    @PostMapping("/productos")
    @Operation(summary = "Crear un producto")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<RespuestaProducto> createProduct(@Valid @RequestBody SolicitudCrearProducto request) {
        RespuestaProducto response = casoUsoCrearProducto.execute(request, ContextoInquilino.getIdTiendaPropia());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/productos")
    @Operation(summary = "Listar productos")
    public ResponseEntity<RespuestaPaginada<RespuestaProducto>> listProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(casoUsoObtenerProducto.listProducts(
                ContextoInquilino.getIdTienda(), PageRequest.of(page, size)));
    }

    @GetMapping("/productos/{enlaceCorto}")
    @Operation(summary = "Obtener producto por enlaceCorto (URL)")
    public ResponseEntity<RespuestaProducto> getProduct(@PathVariable String enlaceCorto) {
        return ResponseEntity.ok(casoUsoObtenerProducto.bySlug(enlaceCorto, ContextoInquilino.getIdTienda()));
    }
}
