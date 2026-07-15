package com.ecommerce.modulos.carrito.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonIgnoreProperties(ignoreUnknown = true)
public class Carrito {

    private static final int MAX_ITEMS = 50;

    private String id;
    private UUID idTienda;
    @Builder.Default
    private List<ArticuloCarrito> articulos = new ArrayList<>();

    public void agregarArticulo(ArticuloCarrito newItem) {
        if (articulos.size() >= MAX_ITEMS) {
            throw new IllegalStateException("Carrito cannot have more than " + MAX_ITEMS + " articulos");
        }

        Optional<ArticuloCarrito> existing = findItem(newItem.getIdProducto(), newItem.getVariantId());
        if (existing.isPresent()) {
            ArticuloCarrito item = existing.get();
            item.setCantidad(item.getCantidad() + newItem.getCantidad());
        } else {
            articulos.add(newItem);
        }
    }

    public void removerArticulo(UUID idProducto, UUID variantId) {
        articulos.removeIf(item ->
                item.getIdProducto().equals(idProducto) &&
                        (variantId == null || variantId.equals(item.getVariantId())));
    }

    public void updateQuantity(UUID idProducto, UUID variantId, int cantidad) {
        if (cantidad <= 0) {
            removerArticulo(idProducto, variantId);
            return;
        }
        findItem(idProducto, variantId).ifPresent(item -> item.setCantidad(cantidad));
    }

    public void clear() {
        articulos.clear();
    }

    public int getItemCount() {
        return articulos.stream().mapToInt(ArticuloCarrito::getCantidad).sum();
    }

    public int getDistinctItemCount() {
        return articulos.size();
    }

    public BigDecimal getTotal() {
        return articulos.stream()
                .map(ArticuloCarrito::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean isEmpty() {
        return articulos.isEmpty();
    }

    private Optional<ArticuloCarrito> findItem(UUID idProducto, UUID variantId) {
        return articulos.stream()
                .filter(item -> {
                    boolean sameProduct = item.getIdProducto().equals(idProducto);
                    boolean sameVariant = (variantId == null && item.getVariantId() == null) ||
                            (variantId != null && variantId.equals(item.getVariantId()));
                    return sameProduct && sameVariant;
                })
                .findFirst();
    }
}
