package com.entitykart.monolith.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockUpdateEvent {
    private Long productId;
    private Integer quantity;
    private String eventType;
}
