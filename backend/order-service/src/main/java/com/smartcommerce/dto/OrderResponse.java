package com.smartcommerce.dto;

import com.smartcommerce.entity.OrderStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private Long id;
    private String orderNumber;
    private String userId;
    private String customerEmail;
    private String customerFirstName;
    private String customerLastName;
    private OrderStatus status;
    private BigDecimal subtotal;
    private BigDecimal shippingCost;
    private BigDecimal tax;
    private BigDecimal total;
    private String shippingStreet;
    private String shippingCity;
    private String shippingCounty;
    private String shippingPostalCode;
    private String shippingCountry;
    private String notes;
    private List<OrderItemResponse> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
