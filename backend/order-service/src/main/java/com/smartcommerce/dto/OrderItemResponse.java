package com.smartcommerce.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String productImage;
    private String productBrand;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
}
