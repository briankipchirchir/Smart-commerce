package com.smartcommerce;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
public class FallbackController {

    @RequestMapping("/fallback/auth")
    public Mono<ResponseEntity<Map<String, String>>> authFallback() {
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "success", "false",
                        "message", "Auth service is temporarily unavailable. Please try again shortly.",
                        "service", "auth-service"
                )));
    }

    @RequestMapping("/fallback/products")
    public Mono<ResponseEntity<Map<String, String>>> productsFallback() {
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "success", "false",
                        "message", "Product service is temporarily unavailable. Please try again shortly.",
                        "service", "product-service"
                )));
    }

    @RequestMapping("/fallback/orders")
    public Mono<ResponseEntity<Map<String, String>>> ordersFallback() {
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "success", "false",
                        "message", "Order service is temporarily unavailable. Please try again shortly.",
                        "service", "order-service"
                )));
    }

    @RequestMapping("/fallback/payments")
    public Mono<ResponseEntity<Map<String, String>>> paymentsFallback() {
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "success", "false",
                        "message", "Payment service is temporarily unavailable. Please try again shortly.",
                        "service", "payment-service"
                )));
    }
}
