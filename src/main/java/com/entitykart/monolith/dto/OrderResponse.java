package com.entitykart.monolith.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class OrderResponse {
    private Long orderId;
    private Long customerId;
    private Long addressId;
    private Double totalAmount;
    private String orderStatus;
    private String paymentStatus;
    private LocalDateTime orderDate;
}
