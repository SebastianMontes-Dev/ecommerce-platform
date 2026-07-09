package com.ecommerce.modules.cart.application;

import com.ecommerce.modules.cart.domain.Cart;
import com.ecommerce.modules.cart.domain.CartItem;
import com.ecommerce.modules.catalog.domain.Product;
import com.ecommerce.modules.catalog.domain.ProductRepository;
import com.ecommerce.modules.shared.domain.EntityNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CartService {

    private static final Logger log = LoggerFactory.getLogger(CartService.class);
    private static final String CART_KEY_PREFIX = "cart:";
    private static final String SESSION_KEY_PREFIX = "cart:session:";
    private static final Duration CART_TTL = Duration.ofDays(7);

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final ProductRepository productRepository;

    public Cart getOrCreateCart(UUID userId, UUID tenantId) {
        String key = CART_KEY_PREFIX + userId;
        Cart cart = loadCart(key);
        if (cart == null) {
            cart = new Cart();
            cart.setId(key);
            cart.setTenantId(tenantId);
        }
        return cart;
    }

    public Cart getOrCreateGuestCart(String sessionId, UUID tenantId) {
        String key = SESSION_KEY_PREFIX + sessionId;
        Cart cart = loadCart(key);
        if (cart == null) {
            cart = new Cart();
            cart.setId(key);
            cart.setTenantId(tenantId);
        }
        return cart;
    }

    public Cart addItem(UUID userId, UUID tenantId, CartItem item) {
        Cart cart = getOrCreateCart(userId, tenantId);
        validateCartItem(item);
        cart.addItem(item);
        saveCart(cart);
        return cart;
    }

    public Cart addItemToGuestCart(String sessionId, UUID tenantId, CartItem item) {
        Cart cart = getOrCreateGuestCart(sessionId, tenantId);
        validateCartItem(item);
        cart.addItem(item);
        saveCart(cart);
        return cart;
    }

    public Cart removeItem(UUID userId, UUID productId, UUID variantId) {
        Cart cart = getOrCreateCart(userId, null);
        cart.removeItem(productId, variantId);
        saveCart(cart);
        return cart;
    }

    public Cart updateQuantity(UUID userId, UUID productId, UUID variantId, int quantity) {
        Cart cart = getOrCreateCart(userId, null);
        cart.updateQuantity(productId, variantId, quantity);
        saveCart(cart);
        return cart;
    }

    public void clearCart(UUID userId) {
        String key = CART_KEY_PREFIX + userId;
        redisTemplate.delete(key);
    }

    public void mergeGuestCartIntoUserCart(String sessionId, UUID userId, UUID tenantId) {
        Cart guestCart = loadCart(SESSION_KEY_PREFIX + sessionId);
        if (guestCart == null || guestCart.isEmpty()) return;

        Cart userCart = getOrCreateCart(userId, tenantId);
        guestCart.getItems().forEach(userCart::addItem);
        saveCart(userCart);
        redisTemplate.delete(SESSION_KEY_PREFIX + sessionId);
    }

    private void validateCartItem(CartItem item) {
        Product product = productRepository.findById(item.getProductId())
                .orElseThrow(() -> new EntityNotFoundException("Product", item.getProductId()));

        if (!product.getTenantId().equals(item.getTenantId())) {
            throw new IllegalStateException("Product does not belong to the specified tenant");
        }
    }

    private Cart loadCart(String key) {
        try {
            Object data = redisTemplate.opsForValue().get(key);
            if (data == null) return null;
            if (data instanceof java.util.LinkedHashMap) {
                return objectMapper.convertValue(data, Cart.class);
            }
            return (Cart) data;
        } catch (Exception e) {
            log.error("Failed to load cart from Redis: {}", key, e);
            return null;
        }
    }

    private void saveCart(Cart cart) {
        try {
            redisTemplate.opsForValue().set(cart.getId(), cart, CART_TTL);
        } catch (Exception e) {
            log.error("Failed to save cart to Redis: {}", cart.getId(), e);
            throw new RuntimeException("Failed to save cart", e);
        }
    }
}
