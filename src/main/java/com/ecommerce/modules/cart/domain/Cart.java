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
    private UUID tenantId;
    @Builder.Default
    private List<CartItem> items = new ArrayList<>();

    public void addItem(CartItem newItem) {
        if (items.size() >= MAX_ITEMS) {
            throw new IllegalStateException("Cart cannot have more than " + MAX_ITEMS + " items");
        }

        Optional<CartItem> existing = findItem(newItem.getProductId(), newItem.getVariantId());
        if (existing.isPresent()) {
            CartItem item = existing.get();
            item.setQuantity(item.getQuantity() + newItem.getQuantity());
        } else {
            items.add(newItem);
        }
    }

    public void removeItem(UUID productId, UUID variantId) {
        items.removeIf(item ->
                item.getProductId().equals(productId) &&
                        (variantId == null || variantId.equals(item.getVariantId())));
    }

    public void updateQuantity(UUID productId, UUID variantId, int quantity) {
        if (quantity <= 0) {
            removeItem(productId, variantId);
            return;
        }
        findItem(productId, variantId).ifPresent(item -> item.setQuantity(quantity));
    }

    public void clear() {
        items.clear();
    }

    public int getItemCount() {
        return items.stream().mapToInt(CartItem::getQuantity).sum();
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

    private Optional<CartItem> findItem(UUID productId, UUID variantId) {
        return items.stream()
                .filter(item -> {
                    boolean sameProduct = item.getProductId().equals(productId);
                    boolean sameVariant = (variantId == null && item.getVariantId() == null) ||
                            (variantId != null && variantId.equals(item.getVariantId()));
                    return sameProduct && sameVariant;
                })
                .findFirst();
    }
}
