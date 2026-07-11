package com.ecommerce.modules.order.application;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private UUID id;
    private String orderNumber;
    private UUID customerId;
    private String customerEmail;
    private String customerName;
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal shippingAmount;
    private BigDecimal total;
    private String currency;
    private String estado;
    private String notes;
    private LocalDateTime creadoEn;
    private LocalDateTime actualizadoEn;
}
