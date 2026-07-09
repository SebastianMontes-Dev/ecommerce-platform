package com.ecommerce.modules.order.application;

import com.ecommerce.modules.order.domain.*;
import com.ecommerce.modules.shared.domain.EntityNotFoundException;
import com.ecommerce.modules.shared.domain.Money;
import com.ecommerce.modules.shared.infrastructure.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderUseCase {

    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID orderId, UUID tenantId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order", orderId));
        if (!order.getTenantId().equals(tenantId)) {
            throw new EntityNotFoundException("Order", orderId);
        }
        return mapToResponse(order);
    }

    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> listOrdersByTenant(UUID tenantId, Pageable pageable) {
        Page<Order> page = orderRepository.findAllByTenantId(tenantId, pageable);
        return PagedResponse.from(page.map(OrderUseCase::mapToResponse));
    }

    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> listOrdersByCustomer(UUID customerId, Pageable pageable) {
        Page<Order> page = orderRepository.findAllByCustomerId(customerId, pageable);
        return PagedResponse.from(page.map(OrderUseCase::mapToResponse));
    }

    @Transactional
    public OrderResponse cancelOrder(UUID orderId, UUID tenantId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order", orderId));
        order.cancel(reason);
        order = orderRepository.save(order);
        return mapToResponse(order);
    }

    static OrderResponse mapToResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .customerId(order.getCustomerId())
                .customerEmail(order.getCustomerEmail())
                .customerName(order.getCustomerName())
                .subtotal(order.getSubtotal() != null ? order.getSubtotal().getAmount() : null)
                .taxAmount(order.getTaxAmount() != null ? order.getTaxAmount().getAmount() : null)
                .shippingAmount(order.getShippingAmount() != null ? order.getShippingAmount().getAmount() : null)
                .total(order.getTotal() != null ? order.getTotal().getAmount() : null)
                .currency(order.getTotal() != null ? order.getTotal().getCurrency() : "USD")
                .status(order.getStatus().name())
                .notes(order.getNotes())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
