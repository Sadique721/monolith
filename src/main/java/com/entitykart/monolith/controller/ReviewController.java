package com.entitykart.monolith.controller;

import com.entitykart.monolith.dto.RatingStatsDTO;
import com.entitykart.monolith.dto.ReviewDTO;
import com.entitykart.monolith.dto.ReviewRequest;
import com.entitykart.monolith.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ReviewDTO createReview(@Valid @RequestBody ReviewRequest request) {
        return reviewService.createReview(request);
    }

    @PutMapping("/{reviewId}")
    public ReviewDTO updateReview(@PathVariable Long reviewId, @Valid @RequestBody ReviewRequest request) {
        return reviewService.updateReview(reviewId, request);
    }

    @DeleteMapping("/{reviewId}")
    public void deleteReview(
            @PathVariable Long reviewId,
            @RequestParam Long customerId,
            @RequestParam(required = false, defaultValue = "false") boolean isAdmin) {
        reviewService.deleteReview(reviewId, customerId, isAdmin);
    }

    @GetMapping("/product/{productId}")
    public Page<ReviewDTO> getProductReviews(@PathVariable Long productId, Pageable pageable) {
        return reviewService.getReviewsByProduct(productId, pageable);
    }

    @GetMapping("/customer/{customerId}")
    public Page<ReviewDTO> getCustomerReviews(@PathVariable Long customerId, Pageable pageable) {
        return reviewService.getReviewsByCustomer(customerId, pageable);
    }

    @GetMapping("/product/{productId}/stats")
    public RatingStatsDTO getProductStats(@PathVariable Long productId) {
        return reviewService.getRatingStats(productId);
    }

    /** Admin — aggregate review stats across all products */
    @GetMapping("/admin/stats")

    public Map<String, Object> getAdminReviewStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalReviews", reviewService.getTotalReviewCount());
        stats.put("averageRating", reviewService.getOverallAverageRating());
        stats.put("fiveStarCount", reviewService.getCountByRating(5));
        stats.put("fourStarCount", reviewService.getCountByRating(4));
        stats.put("threeStarCount", reviewService.getCountByRating(3));
        stats.put("twoStarCount", reviewService.getCountByRating(2));
        stats.put("oneStarCount", reviewService.getCountByRating(1));
        return stats;
    }

    /** Admin — rating distribution for chart */
    @GetMapping("/admin/distribution")

    public Map<String, Object> getAdminReviewDistribution() {
        Map<String, Object> dist = new HashMap<>();
        dist.put("1", reviewService.getCountByRating(1));
        dist.put("2", reviewService.getCountByRating(2));
        dist.put("3", reviewService.getCountByRating(3));
        dist.put("4", reviewService.getCountByRating(4));
        dist.put("5", reviewService.getCountByRating(5));
        return dist;
    }
}

