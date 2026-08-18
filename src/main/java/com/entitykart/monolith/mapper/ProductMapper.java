package com.entitykart.monolith.mapper;

import com.entitykart.monolith.dto.ProductDTO;
import com.entitykart.monolith.entity.ProductEntity;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public ProductDTO toDTO(ProductEntity entity) {
        if (entity == null) {
            return null;
        }
        ProductDTO dto = new ProductDTO();
        dto.setProductId(entity.getProductId());
        dto.setProductName(entity.getProductName());
        dto.setDescription(entity.getDescription());
        dto.setBrand(entity.getBrand());
        dto.setPrice(entity.getPrice());
        dto.setMrp(entity.getMrp());
        dto.setStockQuantity(entity.getStockQuantity());
        dto.setSku(entity.getSku());
        dto.setMainImageURL(entity.getMainImageURL());
        dto.setCategoryId(entity.getCategoryId());
        dto.setSubCategoryId(entity.getSubCategoryId());
        dto.setSellerId(entity.getSellerId());
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    public ProductEntity toEntity(ProductDTO dto) {
        if (dto == null) {
            return null;
        }
        ProductEntity entity = new ProductEntity();
        entity.setProductId(dto.getProductId());
        entity.setProductName(dto.getProductName());
        entity.setDescription(dto.getDescription());
        entity.setBrand(dto.getBrand());
        entity.setPrice(dto.getPrice());
        entity.setMrp(dto.getMrp());
        entity.setStockQuantity(dto.getStockQuantity());
        entity.setSku(dto.getSku());
        entity.setMainImageURL(dto.getMainImageURL());
        entity.setCategoryId(dto.getCategoryId());
        entity.setSubCategoryId(dto.getSubCategoryId());
        entity.setSellerId(dto.getSellerId());
        entity.setStatus(dto.getStatus());
        return entity;
    }
}
