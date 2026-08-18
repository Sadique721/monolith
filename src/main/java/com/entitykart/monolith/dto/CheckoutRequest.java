package com.entitykart.monolith.dto;

import lombok.Data;

@Data
public class CheckoutRequest {
    private Long customerId;
    private Long addressId;
    private String paymentMode;
}
