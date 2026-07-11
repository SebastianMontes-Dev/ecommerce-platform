package com.ecommerce.modules.cart.infrastructure;

import com.ecommerce.modules.cart.application.CartService;
import com.ecommerce.modules.cart.domain.Cart;
import com.ecommerce.modules.cart.domain.CartItem;
import com.ecommerce.modules.identity.application.CustomUserDetails;
import com.ecommerce.modules.shared.infrastructure.TenantContext;
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
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Shopping cart management")
public class CartController {

    private final CartService cartService;

    @GetMapping
    @Operation(summary = "Get current cart")
    public ResponseEntity<Cart> getCart(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpSession session) {

        Cart cart;
        if (userDetails != null) {
            cart = cartService.getOrCreateCart(userDetails.getUserId(), TenantContext.getIdTienda());
        } else {
            cart = cartService.getOrCreateGuestCart(session.getId(), TenantContext.getIdTienda());
        }
        return ResponseEntity.ok(cart);
    }

    @PostMapping("/items")
    @Operation(summary = "Add item to cart")
    public ResponseEntity<Cart> addItem(
            @RequestBody CartItem item,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpSession session) {

        item.setIdTienda(TenantContext.getIdTienda());

        Cart cart;
        if (userDetails != null) {
            cart = cartService.addItem(userDetails.getUserId(), TenantContext.getIdTienda(), item);
        } else {
            cart = cartService.addItemToGuestCart(session.getId(), TenantContext.getIdTienda(), item);
        }
        return ResponseEntity.ok(cart);
    }

    @PutMapping("/items/{idProducto}")
    @Operation(summary = "Update item cantidad")
    public ResponseEntity<Cart> updateQuantity(
            @PathVariable UUID idProducto,
            @RequestParam(defaultValue = "0") int cantidad,
            @RequestParam(required = false) UUID variantId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Cart cart = cartService.updateQuantity(userDetails.getUserId(), idProducto, variantId, cantidad);
        return ResponseEntity.ok(cart);
    }

    @DeleteMapping("/items/{idProducto}")
    @Operation(summary = "Remove item from cart")
    public ResponseEntity<Cart> removeItem(
            @PathVariable UUID idProducto,
            @RequestParam(required = false) UUID variantId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Cart cart = cartService.removeItem(userDetails.getUserId(), idProducto, variantId);
        return ResponseEntity.ok(cart);
    }

    @DeleteMapping
    @Operation(summary = "Clear cart")
    public ResponseEntity<Map<String, String>> clearCart(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        cartService.clearCart(userDetails.getUserId());
        return ResponseEntity.ok(Map.of("message", "Cart cleared"));
    }
}
