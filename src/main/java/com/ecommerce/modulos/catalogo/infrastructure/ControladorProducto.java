package com.ecommerce.modulos.catalogo.infrastructure;

import com.ecommerce.modulos.catalogo.domain.ImagenProducto;
import com.ecommerce.modulos.catalogo.domain.Producto;
import com.ecommerce.modulos.catalogo.domain.RepositorioImagenProducto;
import com.ecommerce.modulos.catalogo.domain.RepositorioProducto;
import com.ecommerce.modulos.compartido.infrastructure.ContextoInquilino;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/catalogo/productos")
@RequiredArgsConstructor
@Tag(name = "Productos", description = "Gestión de productos y sus recursos")
public class ControladorProducto {

    private final ServicioAlmacenamientoCloud servicioAlmacenamientoCloud;
    private final RepositorioProducto repositorioProducto;
    private final RepositorioImagenProducto repositorioImagenProducto;

    @PostMapping("/{id}/imagenes")
    @Operation(summary = "Subir una imagen para un producto")
    @Transactional
    public ResponseEntity<String> uploadImage(@PathVariable UUID id, @RequestParam("file") MultipartFile file) {
        UUID idTienda = ContextoInquilino.getIdTienda();

        Producto producto = repositorioProducto.findByIdForUpdate(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        if (!producto.getIdTienda().equals(idTienda)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Acceso denegado");
        }

        String imageUrl = servicioAlmacenamientoCloud.uploadImage(file);

        ImagenProducto imagenProducto = new ImagenProducto();
        imagenProducto.setIdTienda(idTienda);
        imagenProducto.setIdProducto(producto.getId());
        imagenProducto.setUrl(imageUrl);
        imagenProducto.setAltText(file.getOriginalFilename());
        // set sortOrder as max existing + 1 or just 0
        int sortOrder = repositorioImagenProducto.findAllByIdProductoOrderBySortOrderAsc(id).size();
        imagenProducto.setSortOrder(sortOrder);
        
        repositorioImagenProducto.save(imagenProducto);

        return ResponseEntity.status(HttpStatus.CREATED).body(imageUrl);
    }
}
