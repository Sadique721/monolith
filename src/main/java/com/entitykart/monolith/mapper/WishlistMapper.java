package com.entitykart.monolith.mapper;

import com.entitykart.monolith.dto.WishlistItemDTO;
import com.entitykart.monolith.entity.WishlistItemEntity;
import org.springframework.stereotype.Component;

@Component
public class WishlistMapper {

    public WishlistItemDTO toDTO(WishlistItemEntity entity, String productName, String productImage, Double price) {
        if (entity == null) {
            return null;
        }
        WishlistItemDTO dto = new WishlistItemDTO();
        dto.setWishlistId(entity.getWishlistId());
        dto.setProductId(entity.getProductId());
        dto.setAddedAt(entity.getAddedAt());
        dto.setProductName(productName);
        dto.setProductImage(productImage);
        dto.setPrice(price);
        return dto;
    }

    public WishlistItemEntity toEntity(WishlistItemDTO dto, Long customerId) {
        if (dto == null) {
            return null;
        }
        WishlistItemEntity entity = new WishlistItemEntity();
        entity.setWishlistId(dto.getWishlistId());
        entity.setCustomerId(customerId);
        entity.setProductId(dto.getProductId());
        entity.setAddedAt(dto.getAddedAt());
        return entity;
    }
}
