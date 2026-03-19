package com.smartcommerce.controller;

import com.smartcommerce.dto.InitiatePaymentRequest;
import com.smartcommerce.dto.PaymentResponse;
import com.smartcommerce.response.ApiResponse;
import com.smartcommerce.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/initiate")
    public ResponseEntity<ApiResponse<PaymentResponse>> initiatePayment(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody InitiatePaymentRequest request) {
        String userId = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        PaymentResponse response = paymentService.initiatePayment(userId, email, request);
        return ResponseEntity.ok(ApiResponse.success("Payment initiated", response));
    }

    @PostMapping("/stripe/webhook")
    public ResponseEntity<Void> stripeWebhook(@RequestBody Map<String, Object> payload) {
        try {
            Map<String, Object> data = (Map<String, Object>) payload.get("data");
            Map<String, Object> object = (Map<String, Object>) data.get("object");
            String orderNumber = (String) ((Map<String, Object>) object.get("metadata")).get("orderNumber");
            String status = (String) object.get("status");
            if ("succeeded".equals(status)) {
                paymentService.handleStripeWebhook(orderNumber);
            }
        } catch (Exception e) {
            log.error("Stripe webhook error: {}", e.getMessage());
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/mpesa/callback")
    public ResponseEntity<Void> mpesaCallback(@RequestBody Map<String, Object> payload) {
        try {
            Map<String, Object> body = (Map<String, Object>) payload.get("Body");
            Map<String, Object> stkCallback = (Map<String, Object>) body.get("stkCallback");
            String checkoutRequestId = (String) stkCallback.get("CheckoutRequestID");
            int resultCode = (int) stkCallback.get("ResultCode");
            boolean success = resultCode == 0;
            String receiptNumber = null;
            if (success) {
                var items = (java.util.List<Map<String, Object>>)
                        ((Map<String, Object>) stkCallback.get("CallbackMetadata")).get("Item");
                for (Map<String, Object> item : items) {
                    if ("MpesaReceiptNumber".equals(item.get("Name"))) {
                        receiptNumber = (String) item.get("Value");
                    }
                }
            }
            paymentService.handleMpesaCallback(checkoutRequestId, success, receiptNumber);
        } catch (Exception e) {
            log.error("M-Pesa callback error: {}", e.getMessage());
        }
        return ResponseEntity.ok().build();
    }
}
