package com.smartcommerce.service;

import com.smartcommerce.dto.*;
import com.smartcommerce.entity.Order;
import com.smartcommerce.entity.OrderItem;
import com.smartcommerce.entity.OrderStatus;
import com.smartcommerce.exception.ResourceNotFoundException;
import com.smartcommerce.exception.UnauthorizedException;
import com.smartcommerce.repository.OrderRepository;
import com.smartcommerce.event.OrderCreatedEvent;
import com.smartcommerce.event.OrderEventPublisher;
import com.smartcommerce.response.PagedResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderEventPublisher eventPublisher;
    private static final BigDecimal TAX_RATE = new BigDecimal("0.08");
    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("50");
    private static final BigDecimal SHIPPING_COST = new BigDecimal("5.99");

    @Transactional
    public OrderResponse createOrder(String userId, String email,
                                     String firstName, String lastName,
                                     CreateOrderRequest request) {
        // Calculate totals
        BigDecimal subtotal = request.getItems().stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal shippingCost = subtotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0
                ? BigDecimal.ZERO : SHIPPING_COST;

        BigDecimal tax = subtotal.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(shippingCost).add(tax);

        // Generate order number
        String orderNumber = generateOrderNumber();

        Order order = Order.builder()
                .orderNumber(orderNumber)
                .userId(userId)
                .customerEmail(email)
                .customerFirstName(firstName)
                .customerLastName(lastName)
                .subtotal(subtotal)
                .shippingCost(shippingCost)
                .tax(tax)
                .total(total)
                .shippingStreet(request.getShippingStreet())
                .shippingCity(request.getShippingCity())
                .shippingCounty(request.getShippingCounty())
                .shippingPostalCode(request.getShippingPostalCode())
                .shippingCountry(request.getShippingCountry() != null
                        ? request.getShippingCountry() : "Kenya")
                .notes(request.getNotes())
                .build();

        List<OrderItem> items = request.getItems().stream()
                .map(i -> OrderItem.builder()
                        .order(order)
                        .productId(i.getProductId())
                        .productName(i.getProductName())
                        .productImage(i.getProductImage())
                        .productBrand(i.getProductBrand())
                        .quantity(i.getQuantity())
                        .unitPrice(i.getUnitPrice())
                        .totalPrice(i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                        .build())
                .toList();

        order.setItems(items);
        Order saved = orderRepository.save(order);
        log.info("Order created: {} for user: {}", orderNumber, userId);
        eventPublisher.publishOrderCreated(OrderCreatedEvent.builder()
                .orderNumber(saved.getOrderNumber())
                .customerEmail(saved.getCustomerEmail())
                .customerFirstName(saved.getCustomerFirstName())
                .customerLastName(saved.getCustomerLastName())
                .total(saved.getTotal())
                .shippingCity(saved.getShippingCity())
                .shippingCountry(saved.getShippingCountry())
                .items(saved.getItems().stream()
                        .map(i -> OrderCreatedEvent.OrderItemEvent.builder()
                                .productName(i.getProductName())
                                .productBrand(i.getProductBrand())
                                .quantity(i.getQuantity())
                                .unitPrice(i.getUnitPrice())
                                .totalPrice(i.getTotalPrice())
                                .build())
                        .toList())
                .build());
        return toResponse(saved);
    }

    public PagedResult<OrderResponse> getMyOrders(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<OrderResponse> result = orderRepository.findByUserId(userId, pageable)
                .map(this::toResponse);
        return PagedResult.from(result);
    }

    public PagedResult<OrderResponse> getAllOrders(int page, int size, String status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<OrderResponse> result;
        if (status != null && !status.isEmpty()) {
            result = orderRepository.findByStatus(OrderStatus.valueOf(status), pageable)
                    .map(this::toResponse);
        } else {
            result = orderRepository.findAll(pageable).map(this::toResponse);
        }
        return PagedResult.from(result);
    }

    public OrderResponse getOrderById(Long id, String userId, boolean isAdmin) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (!isAdmin && !order.getUserId().equals(userId)) {
            throw new UnauthorizedException("Access denied");
        }
        return toResponse(order);
    }

    public OrderResponse getOrderByNumber(String orderNumber, String userId, boolean isAdmin) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (!isAdmin && !order.getUserId().equals(userId)) {
            throw new UnauthorizedException("Access denied");
        }
        return toResponse(order);
    }

    @Transactional
    public OrderResponse updateStatus(Long id, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        order.setStatus(request.getStatus());
        return toResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse cancelOrder(Long id, String userId, boolean isAdmin) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (!isAdmin && !order.getUserId().equals(userId)) {
            throw new UnauthorizedException("Access denied");
        }
        if (order.getStatus() == OrderStatus.DELIVERED ||
            order.getStatus() == OrderStatus.SHIPPED) {
            throw new RuntimeException("Cannot cancel a shipped or delivered order");
        }
        order.setStatus(OrderStatus.CANCELLED);
        return toResponse(orderRepository.save(order));
    }

    public java.util.Map<String, Long> getOrderStats() {
        java.util.Map<String, Long> stats = new java.util.HashMap<>();
        for (OrderStatus status : OrderStatus.values()) {
            stats.put(status.name(), orderRepository.countByStatus(status));
        }
        stats.put("TOTAL", orderRepository.count());
        return stats;
    }

    private String generateOrderNumber() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int random = new Random().nextInt(9000) + 1000;
        return "ORD-" + timestamp + "-" + random;
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getItems() == null ? List.of() :
                order.getItems().stream()
                        .map(i -> OrderItemResponse.builder()
                                .id(i.getId())
                                .productId(i.getProductId())
                                .productName(i.getProductName())
                                .productImage(i.getProductImage())
                                .productBrand(i.getProductBrand())
                                .quantity(i.getQuantity())
                                .unitPrice(i.getUnitPrice())
                                .totalPrice(i.getTotalPrice())
                                .build())
                        .toList();

        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUserId())
                .customerEmail(order.getCustomerEmail())
                .customerFirstName(order.getCustomerFirstName())
                .customerLastName(order.getCustomerLastName())
                .status(order.getStatus())
                .subtotal(order.getSubtotal())
                .shippingCost(order.getShippingCost())
                .tax(order.getTax())
                .total(order.getTotal())
                .shippingStreet(order.getShippingStreet())
                .shippingCity(order.getShippingCity())
                .shippingCounty(order.getShippingCounty())
                .shippingPostalCode(order.getShippingPostalCode())
                .shippingCountry(order.getShippingCountry())
                .notes(order.getNotes())
                .items(items)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
