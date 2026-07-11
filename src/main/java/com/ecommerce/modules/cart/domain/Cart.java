package com.ecommerce.modules.cart.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cart {

    private static final int MAX_ITEMS = 50;

    private String id;
    private UUID idTienda;
    @Builder.Default
    private List<CartItem> items = new ArrayList<>();

    public void addItem(CartItem newItem) {
        if (items.size() >= MAX_ITEMS) {
            throw new IllegalStateException("Cart cannot have more than " + MAX_ITEMS + " items");
        }

        Optional<CartItem> existing = findItem(newItem.getIdProducto(), newItem.getVariantId());
        if (existing.isPresent()) {
            CartItem item = existing.get();
            item.setCantidad(item.getCantidad() + newItem.getCantidad());
        } else {
            items.add(newItem);
        }
    }

    public void removeItem(UUID idProducto, UUID variantId) {
        items.removeIf(item ->
                item.getIdProducto().equals(idProducto) &&
                        (variantId == null || variantId.equals(item.getVariantId())));
    }

    public void updateQuantity(UUID idProducto, UUID variantId, int cantidad) {
        if (cantidad <= 0) {
            removeItem(idProducto, variantId);
            return;
        }
        findItem(idProducto, variantId).ifPresent(item -> item.setCantidad(cantidad));
    }

    public void clear() {
        items.clear();
    }

    public int getItemCount() {
        return items.stream().mapToInt(CartItem::getCantidad).sum();
    }

    public int getDistinctItemCount() {
        return items.size();
    }

    public BigDecimal getTotal() {
        return items.stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    private Optional<CartItem> findItem(UUID idProducto, UUID variantId) {
        return items.stream()
                .filter(item -> {
                    boolean sameProduct = item.getIdProducto().equals(idProducto);
                    boolean sameVariant = (variantId == null && item.getVariantId() == null) ||
                            (variantId != null && variantId.equals(item.getVariantId()));
                    return sameProduct && sameVariant;
                })
                .findFirst();
    }
}
