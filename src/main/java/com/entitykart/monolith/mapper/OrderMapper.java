package com.entitykart.monolith.mapper;

import com.entitykart.monolith.dto.OrderDTO;
import com.entitykart.monolith.dto.OrderItemDTO;
import com.entitykart.monolith.entity.OrderEntity;
import com.entitykart.monolith.entity.OrderItemEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class OrderMapper {

    public OrderDTO toDTO(OrderEntity entity, List<OrderItemEntity> items) {
        if (entity == null) {
            return null;
        }
        OrderDTO dto = new OrderDTO();
        dto.setOrderId(entity.getOrderId());
        dto.setCustomerId(entity.getCustomerId());
        dto.setAddressId(entity.getAddressId());
        dto.setTotalAmount(entity.getTotalAmount());
        dto.setOrderStatus(entity.getOrderStatus().name());
        dto.setPaymentStatus(entity.getPaymentStatus().name());
        dto.setOrderDate(entity.getOrderDate());
        dto.setCreatedAt(entity.getCreatedAt());

        if (items != null) {
            dto.setItems(items.stream().map(item -> {
                OrderDTO.OrderItemDTO d = new OrderDTO.OrderItemDTO();
                d.setProductId(item.getProductId());
                d.setQuantity(item.getQuantity());
                d.setPrice(item.getPrice());
                d.setSubtotal(item.getQuantity() * item.getPrice());
                return d;
            }).collect(Collectors.toList()));
        }
        return dto;
    }

}
