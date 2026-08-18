package com.entitykart.monolith.controller;

import com.entitykart.monolith.dto.WishlistItemDTO;
import com.entitykart.monolith.service.WishlistService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    private void verifyOwnership(Long loggedInId, String role, Long requestedId) {
        if (loggedInId != null && !requestedId.equals(loggedInId) && !"ADMIN".equalsIgnoreCase(role)) {
            throw new RuntimeException("Unauthorized: cannot access another customer's wishlist");
        }
    }

    @PostMapping("/add")
    public void addToWishlist(
            @RequestHeader(value = "X-Customer-Id", required = false) Long loggedInCustomerId,
            @RequestHeader(value = "X-User-Role",   required = false) String loggedInUserRole,
            @RequestParam Long customerId,
            @RequestParam Long productId) {
        verifyOwnership(loggedInCustomerId, loggedInUserRole, customerId);
        wishlistService.addToWishlist(customerId, productId);
    }

    @DeleteMapping("/remove")
    public void removeFromWishlist(
            @RequestHeader(value = "X-Customer-Id", required = false) Long loggedInCustomerId,
            @RequestHeader(value = "X-User-Role",   required = false) String loggedInUserRole,
            @RequestParam Long customerId,
            @RequestParam Long productId) {
        verifyOwnership(loggedInCustomerId, loggedInUserRole, customerId);
        wishlistService.removeFromWishlist(customerId, productId);
    }

    @DeleteMapping("/clear")
    public void clearWishlist(
            @RequestHeader(value = "X-Customer-Id", required = false) Long loggedInCustomerId,
            @RequestHeader(value = "X-User-Role",   required = false) String loggedInUserRole,
            @RequestParam Long customerId) {
        verifyOwnership(loggedInCustomerId, loggedInUserRole, customerId);
        wishlistService.clearWishlist(customerId);
    }

    @GetMapping
    public List<WishlistItemDTO> getWishlist(
            @RequestHeader(value = "X-Customer-Id", required = false) Long loggedInCustomerId,
            @RequestHeader(value = "X-User-Role",   required = false) String loggedInUserRole,
            @RequestParam Long customerId) {
        verifyOwnership(loggedInCustomerId, loggedInUserRole, customerId);
        return wishlistService.getWishlist(customerId);
    }

    @GetMapping("/paginated")
    public Page<WishlistItemDTO> getWishlistPaginated(
            @RequestHeader(value = "X-Customer-Id", required = false) Long loggedInCustomerId,
            @RequestHeader(value = "X-User-Role",   required = false) String loggedInUserRole,
            @RequestParam Long customerId,
            Pageable pageable) {
        verifyOwnership(loggedInCustomerId, loggedInUserRole, customerId);
        return wishlistService.getWishlistPaginated(customerId, pageable);
    }

    @GetMapping("/all")
    public List<WishlistItemDTO> getAllWishlistItems(
            @RequestHeader(value = "X-User-Role", required = false) String loggedInUserRole) {
        if (!"ADMIN".equalsIgnoreCase(loggedInUserRole)) {
            throw new RuntimeException("Access Denied: Admin role required");
        }
        return wishlistService.getAllWishlistItems();
    }
}
