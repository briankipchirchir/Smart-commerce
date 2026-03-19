package com.smartcommerce.dto;

import com.smartcommerce.entity.PaymentMethod;
import com.smartcommerce.entity.PaymentStatus;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentResponse {
    private Long id;
    private String orderNumber;
    private BigDecimal amount;
    private PaymentMethod method;
    private PaymentStatus status;
    private String stripeClientSecret; // for Stripe frontend
    private String mpesaCheckoutRequestId;
    private LocalDateTime createdAt;
}
