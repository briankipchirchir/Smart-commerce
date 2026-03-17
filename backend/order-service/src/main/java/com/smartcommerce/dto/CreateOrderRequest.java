package com.smartcommerce.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class CreateOrderRequest {
    @NotEmpty(message = "Order must have at least one item")
    private List<OrderItemRequest> items;

    private String shippingStreet;
    private String shippingCity;
    private String shippingCounty;
    private String shippingPostalCode;
    private String shippingCountry;
    private String notes;
}
