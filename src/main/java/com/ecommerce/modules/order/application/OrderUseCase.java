package com.ecommerce.modules.order.application;

import com.ecommerce.modules.order.domain.*;
import com.ecommerce.modules.shared.domain.EntityNotFoundException;
import com.ecommerce.modules.shared.domain.Money;
import com.ecommerce.modules.shared.domain.DomainEventPublisher;
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
    private final DomainEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID idOrden, UUID idTienda) {
        Order order = orderRepository.findById(idOrden)
                .orElseThrow(() -> new EntityNotFoundException("Order", idOrden));
        if (!order.getIdTienda().equals(idTienda)) {
            throw new EntityNotFoundException("Order", idOrden);
        }
        return mapToResponse(order);
    }

    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> listOrdersByTenant(UUID idTienda, Pageable pageable) {
        Page<Order> page = orderRepository.findAllByTenantId(idTienda, pageable);
        return PagedResponse.from(page.map(OrderUseCase::mapToResponse));
    }

    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> listOrdersByCustomer(UUID customerId, Pageable pageable) {
        Page<Order> page = orderRepository.findAllByCustomerId(customerId, pageable);
        return PagedResponse.from(page.map(OrderUseCase::mapToResponse));
    }

    @Transactional
    public OrderResponse cancelOrder(UUID idOrden, UUID idTienda, String reason) {
        Order order = orderRepository.findById(idOrden)
                .orElseThrow(() -> new EntityNotFoundException("Order", idOrden));
        order.cancel(reason);
        order = orderRepository.save(order);
        eventPublisher.publish(order.getDomainEvents());
        order.clearDomainEvents();
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
                .estado(order.getEstado().name())
                .notes(order.getNotes())
                .creadoEn(order.getCreadoEn())
                .actualizadoEn(order.getActualizadoEn())
                .build();
    }
}
