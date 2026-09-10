package com.ecommerce.modulos.carrito.application;

import com.ecommerce.modulos.carrito.domain.Carrito;
import com.ecommerce.modulos.carrito.domain.ArticuloCarrito;
import com.ecommerce.modulos.catalogo.domain.Producto;
import com.ecommerce.modulos.catalogo.domain.RepositorioProducto;
import com.ecommerce.modulos.compartido.domain.ExcepcionEntidadNoEncontrada;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ServicioCarrito {

    private static final Logger log = LoggerFactory.getLogger(ServicioCarrito.class);
    private static final String CART_KEY_PREFIX = "carrito:";
    private static final Duration CART_TTL = Duration.ofDays(7);

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final RepositorioProducto repositorioProducto;

    /**
     * Los usuarios son identidades globales (no pertenecen a un inquilino), así que el
     * mismo {@code userId} puede comprar en varias tiendas. La clave del carrito DEBE
     * incluir la tienda o el carrito de una tienda contamina el de otra.
     */
    private String claveCarrito(UUID idTienda, UUID userId) {
        return CART_KEY_PREFIX + idTienda + ":" + userId;
    }

    private String claveCarritoInvitado(UUID idTienda, String sessionId) {
        return CART_KEY_PREFIX + idTienda + ":session:" + sessionId;
    }

    public Carrito getOrCreateCart(UUID userId, UUID idTienda) {
        String key = claveCarrito(idTienda, userId);
        Carrito carrito = loadCart(key);
        if (carrito == null) {
            carrito = new Carrito();
            carrito.setId(key);
            carrito.setIdTienda(idTienda);
        }
        return carrito;
    }

    public Carrito getOrCreateGuestCart(String sessionId, UUID idTienda) {
        String key = claveCarritoInvitado(idTienda, sessionId);
        Carrito carrito = loadCart(key);
        if (carrito == null) {
            carrito = new Carrito();
            carrito.setId(key);
            carrito.setIdTienda(idTienda);
        }
        return carrito;
    }

    public Carrito agregarArticulo(UUID userId, UUID idTienda, ArticuloCarrito item) {
        Carrito carrito = getOrCreateCart(userId, idTienda);
        validateCartItem(item);
        carrito.agregarArticulo(item);
        saveCart(carrito);
        return carrito;
    }

    public Carrito addItemToGuestCart(String sessionId, UUID idTienda, ArticuloCarrito item) {
        Carrito carrito = getOrCreateGuestCart(sessionId, idTienda);
        validateCartItem(item);
        carrito.agregarArticulo(item);
        saveCart(carrito);
        return carrito;
    }

    public Carrito removerArticulo(UUID userId, UUID idTienda, UUID idProducto, UUID variantId) {
        Carrito carrito = getOrCreateCart(userId, idTienda);
        carrito.removerArticulo(idProducto, variantId);
        saveCart(carrito);
        return carrito;
    }

    public Carrito updateQuantity(UUID userId, UUID idTienda, UUID idProducto, UUID variantId, int cantidad) {
        Carrito carrito = getOrCreateCart(userId, idTienda);
        carrito.updateQuantity(idProducto, variantId, cantidad);
        saveCart(carrito);
        return carrito;
    }

    public void clearCart(UUID userId, UUID idTienda) {
        redisTemplate.delete(claveCarrito(idTienda, userId));
    }

    public Carrito aplicarCupon(UUID userId, String sessionId, UUID idTienda, String codigoCupon, java.math.BigDecimal montoDescuento) {
        Carrito carrito = userId != null
                ? getOrCreateCart(userId, idTienda)
                : getOrCreateGuestCart(sessionId, idTienda);
        carrito.setCodigoCupon(codigoCupon);
        carrito.setMontoDescuento(montoDescuento);
        saveCart(carrito);
        return carrito;
    }

    public void mergeGuestCartIntoUserCart(String sessionId, UUID userId, UUID idTienda) {
        Carrito carritoInvitado = loadCart(claveCarritoInvitado(idTienda, sessionId));
        if (carritoInvitado == null || carritoInvitado.isEmpty()) return;

        Carrito carritoUsuario = getOrCreateCart(userId, idTienda);
        carritoInvitado.getArticulos().forEach(carritoUsuario::agregarArticulo);
        saveCart(carritoUsuario);
        redisTemplate.delete(claveCarritoInvitado(idTienda, sessionId));
    }

    private void validateCartItem(ArticuloCarrito item) {
        Producto producto = repositorioProducto.findByIdWithVariants(item.getIdProducto())
                .orElseThrow(() -> new ExcepcionEntidadNoEncontrada("Producto", item.getIdProducto()));

        if (!producto.getIdTienda().equals(item.getIdTienda())) {
            throw new IllegalStateException("Producto does not belong to the specified inquilino");
        }

        if (item.getVariantId() != null) {
            com.ecommerce.modulos.catalogo.domain.VarianteProducto variante = producto.getVariants().stream()
                    .filter(v -> v.getId().equals(item.getVariantId()))
                    .findFirst()
                    .orElseThrow(() -> new ExcepcionEntidadNoEncontrada("VarianteProducto", item.getVariantId()));

            item.setPrecioUnitario(variante.getPrecio().getMonto());
            item.setVariantName(variante.getNombre());
            item.setNombreProducto(producto.getNombre());
            item.setMoneda(variante.getPrecio().getMoneda());
        } else {
            item.setPrecioUnitario(producto.getPrecio().getMonto());
            item.setNombreProducto(producto.getNombre());
            item.setMoneda(producto.getPrecio().getMoneda());
        }
    }

    private Carrito loadCart(String key) {
        try {
            Object data = redisTemplate.opsForValue().get(key);
            if (data == null) return null;
            if (data instanceof java.util.LinkedHashMap) {
                return objectMapper.convertValue(data, Carrito.class);
            }
            return (Carrito) data;
        } catch (Exception e) {
            log.error("Failed to load carrito from Redis: {}", key, e);
            return null;
        }
    }

    private void saveCart(Carrito carrito) {
        try {
            redisTemplate.opsForValue().set(carrito.getId(), carrito, CART_TTL);
        } catch (Exception e) {
            log.error("Failed to save carrito to Redis: {}", carrito.getId(), e);
            throw new RuntimeException("Failed to save carrito", e);
        }
    }
}
