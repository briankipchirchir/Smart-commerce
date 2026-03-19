package com.smartcommerce.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent implements Serializable {
    private String orderNumber;
    private String customerEmail;
    private String customerFirstName;
    private String customerLastName;
    private BigDecimal total;
    private String shippingCity;
    private String shippingCountry;
}
