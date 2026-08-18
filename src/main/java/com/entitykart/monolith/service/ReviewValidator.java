package com.entitykart.monolith.service;

import com.entitykart.monolith.service.OrderService;
import com.entitykart.monolith.entity.ReviewEntity;
import com.entitykart.monolith.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReviewValidator {

    private final ReviewRepository reviewRepository;
    private final OrderService orderService;

    public void validatePurchase(Long customerId, Long productId) {
        if (!orderService.hasCustomerPurchasedProduct(customerId, productId)) {
            throw new RuntimeException("You can only review products you have purchased");
        }
    }

    public void validateNotAlreadyReviewed(Long customerId, Long productId) {
        if (reviewRepository.existsByCustomerIdAndProductId(customerId, productId)) {
            throw new RuntimeException("You have already reviewed this product");
        }
    }

    public void validateOwner(ReviewEntity review, Long customerId) {
        if (!review.getCustomerId().equals(customerId)) {
            throw new RuntimeException("You can only edit your own review");
        }
    }

    public void validateDeletePermission(ReviewEntity review, Long customerId, boolean isAdmin) {
        if (!isAdmin && !review.getCustomerId().equals(customerId)) {
            throw new RuntimeException("Unauthorized");
        }
    }
}
