package com.ecommerce.modulos.carrito.infrastructure;

import com.ecommerce.modulos.carrito.application.ServicioCarrito;
import com.ecommerce.modulos.carrito.domain.Carrito;
import com.ecommerce.modulos.carrito.domain.ArticuloCarrito;
import com.ecommerce.modulos.identidad.application.DetallesUsuarioPersonalizado;
import com.ecommerce.modulos.compartido.infrastructure.ContextoInquilino;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/carrito")
@RequiredArgsConstructor
@Tag(name = "Carrito", description = "Shopping carrito management")
public class ControladorCarrito {

    private final ServicioCarrito servicioCarrito;

    @GetMapping
    @Operation(summary = "Get current carrito")
    public ResponseEntity<Carrito> getCarrito(
            @AuthenticationPrincipal DetallesUsuarioPersonalizado userDetails,
            HttpSession session) {

        Carrito carrito;
        if (userDetails != null) {
            carrito = servicioCarrito.getOrCreateCart(userDetails.getUserId(), ContextoInquilino.getIdTienda());
        } else {
            carrito = servicioCarrito.getOrCreateGuestCart(session.getId(), ContextoInquilino.getIdTienda());
        }
        return ResponseEntity.ok(carrito);
    }

    @PostMapping("/articulos")
    @Operation(summary = "Add item to carrito")
    public ResponseEntity<Carrito> agregarArticulo(
            @RequestBody ArticuloCarrito item,
            @AuthenticationPrincipal DetallesUsuarioPersonalizado userDetails,
            HttpSession session) {

        item.setIdTienda(ContextoInquilino.getIdTienda());

        Carrito carrito;
        if (userDetails != null) {
            carrito = servicioCarrito.agregarArticulo(userDetails.getUserId(), ContextoInquilino.getIdTienda(), item);
        } else {
            carrito = servicioCarrito.addItemToGuestCart(session.getId(), ContextoInquilino.getIdTienda(), item);
        }
        return ResponseEntity.ok(carrito);
    }

    @PutMapping("/articulos/{idProducto}")
    @Operation(summary = "Update item cantidad")
    public ResponseEntity<Carrito> updateQuantity(
            @PathVariable UUID idProducto,
            @RequestParam(defaultValue = "0") int cantidad,
            @RequestParam(required = false) UUID variantId,
            @AuthenticationPrincipal DetallesUsuarioPersonalizado userDetails) {

        Carrito carrito = servicioCarrito.updateQuantity(userDetails.getUserId(), idProducto, variantId, cantidad);
        return ResponseEntity.ok(carrito);
    }

    @DeleteMapping("/articulos/{idProducto}")
    @Operation(summary = "Remove item from carrito")
    public ResponseEntity<Carrito> removerArticulo(
            @PathVariable UUID idProducto,
            @RequestParam(required = false) UUID variantId,
            @AuthenticationPrincipal DetallesUsuarioPersonalizado userDetails) {

        Carrito carrito = servicioCarrito.removerArticulo(userDetails.getUserId(), idProducto, variantId);
        return ResponseEntity.ok(carrito);
    }

    @DeleteMapping
    @Operation(summary = "Clear carrito")
    public ResponseEntity<Map<String, String>> clearCart(
            @AuthenticationPrincipal DetallesUsuarioPersonalizado userDetails) {

        servicioCarrito.clearCart(userDetails.getUserId());
        return ResponseEntity.ok(Map.of("message", "Carrito cleared"));
    }
}
