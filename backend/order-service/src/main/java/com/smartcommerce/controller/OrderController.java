package com.smartcommerce.controller;

import com.smartcommerce.dto.*;
import com.smartcommerce.response.ApiResponse;
import com.smartcommerce.response.PagedResult;
import com.smartcommerce.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateOrderRequest request) {
        String userId = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        String firstName = jwt.getClaimAsString("given_name");
        String lastName = jwt.getClaimAsString("family_name");
        OrderResponse order = orderService.createOrder(userId, email, firstName, lastName, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order created successfully", order));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<PagedResult<OrderResponse>>> getMyOrders(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        String userId = jwt.getSubject();
        return ResponseEntity.ok(ApiResponse.success("Orders retrieved",
                orderService.getMyOrders(userId, page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") Long id) {
        String userId = jwt.getSubject();
        boolean isAdmin = isAdmin(jwt);
        return ResponseEntity.ok(ApiResponse.success("Order retrieved",
                orderService.getOrderById(id, userId, isAdmin)));
    }

    @GetMapping("/number/{orderNumber}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderByNumber(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("orderNumber") String orderNumber) {
        String userId = jwt.getSubject();
        boolean isAdmin = isAdmin(jwt);
        return ResponseEntity.ok(ApiResponse.success("Order retrieved",
                orderService.getOrderByNumber(orderNumber, userId, isAdmin)));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") Long id) {
        String userId = jwt.getSubject();
        boolean isAdmin = isAdmin(jwt);
        return ResponseEntity.ok(ApiResponse.success("Order cancelled",
                orderService.cancelOrder(id, userId, isAdmin)));
    }

    // ── Admin endpoints ──────────────────────────────────
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PagedResult<OrderResponse>>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(ApiResponse.success("Orders retrieved",
                orderService.getAllOrders(page, size, status)));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> updateStatus(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Order status updated",
                orderService.updateStatus(id, request)));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getOrderStats() {
        return ResponseEntity.ok(ApiResponse.success("Stats retrieved",
                orderService.getOrderStats()));
    }

    private boolean isAdmin(Jwt jwt) {
        try {
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            if (realmAccess != null) {
                List<String> roles = (List<String>) realmAccess.get("roles");
                return roles != null && roles.contains("ADMIN");
            }
        } catch (Exception ignored) {}
        return false;
    }
}
