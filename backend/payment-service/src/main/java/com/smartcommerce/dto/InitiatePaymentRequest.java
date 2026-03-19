package com.smartcommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class InitiatePaymentRequest {
    @NotBlank private String orderNumber;
    @NotNull private BigDecimal amount;
    @NotBlank private String method; // STRIPE, MPESA, COD
    private String mpesaPhone; // required for MPESA
}
