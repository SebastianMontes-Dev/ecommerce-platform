package com.ecommerce.modules.order.infrastructure;

import com.ecommerce.modules.order.application.*;
import com.ecommerce.modules.shared.infrastructure.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order management")
public class OrderController {

    private final OrderUseCase orderUseCase;

    @GetMapping
    @Operation(summary = "List orders for current tenant")
    public ResponseEntity<?> listOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(orderUseCase.listOrdersByTenant(
                TenantContext.getIdTienda(),
                org.springframework.data.domain.PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable UUID id) {
        return ResponseEntity.ok(orderUseCase.getOrder(id, TenantContext.getIdTienda()));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel an order")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable UUID id, @RequestParam(defaultValue = "") String reason) {
        return ResponseEntity.ok(orderUseCase.cancelOrder(id, TenantContext.getIdTienda(), reason));
    }
}
