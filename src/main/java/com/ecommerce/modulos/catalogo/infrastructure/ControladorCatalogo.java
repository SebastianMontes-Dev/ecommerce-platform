package com.ecommerce.modulos.catalogo.infrastructure;

import com.ecommerce.modulos.catalogo.application.*;
import com.ecommerce.modulos.catalogo.application.dto.*;
import com.ecommerce.modulos.compartido.infrastructure.ContextoInquilino;
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
@RequestMapping("/api/v1/catalogo")
@RequiredArgsConstructor
@Tag(name = "Catálogo", description = "Gestión del catálogo de productos")
public class ControladorCatalogo {

    private final CasoUsoCrearProducto casoUsoCrearProducto;
    private final CasoUsoObtenerProducto casoUsoObtenerProducto;
    private final CasoUsoCrearCategoria casoUsoCrearCategoria;

    @PostMapping("/categorias")
    @Operation(summary = "Crear una categoría")
    @CacheEvict(value = "categorias", key = "T(com.ecommerce.modulos.compartido.infrastructure.ContextoInquilino).getIdTienda()")
    public ResponseEntity<RespuestaCategoria> createCategory(@Valid @RequestBody SolicitudCrearCategoria request) {
        RespuestaCategoria response = casoUsoCrearCategoria.execute(request, ContextoInquilino.getIdTienda());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/categorias")
    @Operation(summary = "Listar categorías")
    @Cacheable(value = "categorias", key = "T(com.ecommerce.modulos.compartido.infrastructure.ContextoInquilino).getIdTienda()")
    public ResponseEntity<List<RespuestaCategoria>> listCategories() {
        return ResponseEntity.ok(casoUsoCrearCategoria.getCategories(ContextoInquilino.getIdTienda()));
    }

    @PostMapping("/productos")
    @Operation(summary = "Crear un producto")
    @CacheEvict(value = {"productos", "product_details"}, allEntries = true) // Limpiar todo el caché de productos por ahora
    public ResponseEntity<RespuestaProducto> createProduct(@Valid @RequestBody SolicitudCrearProducto request) {
        RespuestaProducto response = casoUsoCrearProducto.execute(request, ContextoInquilino.getIdTienda());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/productos")
    @Operation(summary = "Listar productos")
    @Cacheable(value = "productos", key = "T(com.ecommerce.modulos.compartido.infrastructure.ContextoInquilino).getIdTienda() + '_' + #page + '_' + #size")
    public ResponseEntity<?> listProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(casoUsoObtenerProducto.listProducts(
                ContextoInquilino.getIdTienda(),
                org.springframework.data.domain.PageRequest.of(page, size)));
    }

    @GetMapping("/productos/{enlaceCorto}")
    @Operation(summary = "Obtener producto por enlaceCorto (URL)")
    @Cacheable(value = "product_details", key = "T(com.ecommerce.modulos.compartido.infrastructure.ContextoInquilino).getIdTienda() + '_' + #enlaceCorto")
    public ResponseEntity<RespuestaProducto> getProduct(@PathVariable String enlaceCorto) {
        return ResponseEntity.ok(casoUsoObtenerProducto.bySlug(enlaceCorto, ContextoInquilino.getIdTienda()));
    }
}
