package com.entitykart.monolith.mapper;

import com.entitykart.monolith.dto.ReviewDTO;
import com.entitykart.monolith.entity.ReviewEntity;
import org.springframework.stereotype.Component;

@Component
public class ReviewMapper {

    public ReviewDTO toDTO(ReviewEntity entity) {
        if (entity == null) {
            return null;
        }
        ReviewDTO dto = new ReviewDTO();
        dto.setReviewId(entity.getReviewId());
        dto.setProductId(entity.getProductId());
        dto.setCustomerId(entity.getCustomerId());
        dto.setRating(entity.getRating());
        dto.setComment(entity.getComment());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    public ReviewEntity toEntity(ReviewDTO dto) {
        if (dto == null) {
            return null;
        }
        ReviewEntity entity = new ReviewEntity();
        entity.setReviewId(dto.getReviewId());
        entity.setProductId(dto.getProductId());
        entity.setCustomerId(dto.getCustomerId());
        entity.setRating(dto.getRating());
        entity.setComment(dto.getComment());
        return entity;
    }
}
