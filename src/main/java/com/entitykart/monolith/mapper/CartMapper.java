package com.entitykart.monolith.mapper;

import com.entitykart.monolith.dto.CartItemDTO;
import com.entitykart.monolith.entity.CartItemEntity;
import org.springframework.stereotype.Component;

@Component
public class CartMapper {

    public CartItemDTO toDTO(CartItemEntity entity, String productName, String mainImageURL) {
        if (entity == null) {
            return null;
        }
        CartItemDTO dto = new CartItemDTO();
        dto.setCartItemId(entity.getCartItemId());
        dto.setProductId(entity.getProductId());
        dto.setQuantity(entity.getQuantity());
        dto.setPrice(entity.getPrice());
        dto.setSubtotal(entity.getQuantity() * entity.getPrice());
        dto.setProductName(productName);
        dto.setMainImageURL(mainImageURL);
        return dto;
    }

    public CartItemEntity toEntity(CartItemDTO dto, Long customerId) {
        if (dto == null) {
            return null;
        }
        CartItemEntity entity = new CartItemEntity();
        entity.setCartItemId(dto.getCartItemId());
        entity.setCustomerId(customerId);
        entity.setProductId(dto.getProductId());
        entity.setQuantity(dto.getQuantity());
        entity.setPrice(dto.getPrice());
        return entity;
    }
}
