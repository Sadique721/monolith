package com.entitykart.monolith.service;

import com.entitykart.monolith.dto.ProductDTO;
import com.entitykart.monolith.dto.WishlistItemDTO;
import com.entitykart.monolith.entity.WishlistItemEntity;
import com.entitykart.monolith.repository.WishlistRepository;
import com.entitykart.monolith.mapper.WishlistMapper;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductService productService;
    private final WishlistMapper wishlistMapper;

    @Transactional
    public void addToWishlist(Long customerId, Long productId) {
        if (customerId == null || productId == null) {
            throw new RuntimeException("Customer ID and Product ID cannot be null");
        }
        if (wishlistRepository.existsByCustomerIdAndProductId(customerId, productId)) {
            throw new RuntimeException("Product already in wishlist");
        }

        WishlistItemEntity item = new WishlistItemEntity();
        item.setCustomerId(customerId);
        item.setProductId(productId);
        wishlistRepository.save(item);

        log.info("Added product {} to wishlist of customer {}", productId, customerId);
    }

    @Transactional
    public void removeFromWishlist(Long customerId, Long productId) {
        wishlistRepository.deleteByCustomerIdAndProductId(customerId, productId);
        log.info("Removed product {} from wishlist of customer {}", productId, customerId);
    }

    @Transactional
    public void clearWishlist(Long customerId) {
        wishlistRepository.deleteByCustomerId(customerId);
        log.info("Cleared wishlist for customer {}", customerId);
    }

    @Transactional(readOnly = true)
    public List<WishlistItemDTO> getWishlist(Long customerId) {
        List<WishlistItemEntity> items = wishlistRepository.findByCustomerId(customerId);
        return items.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<WishlistItemDTO> getWishlistPaginated(Long customerId, Pageable pageable) {
        Page<WishlistItemEntity> page = wishlistRepository.findByCustomerIdOrderByAddedAtDesc(customerId, pageable);
        return page.map(this::convertToDTO);
    }

    private WishlistItemDTO convertToDTO(WishlistItemEntity entity) {
        String prodName = "Unknown Product";
        String prodImage = null;
        Double price = null;

        try {
            ProductDTO product = productService.getProduct(entity.getProductId());
            if (product != null) {
                prodName = product.getProductName();
                prodImage = product.getMainImageURL();
                if (product.getPrice() != null) {
                    price = product.getPrice().doubleValue();
                }
            }
        } catch (Exception exception) {
            log.error("Failed to fetch product details for id: {}", entity.getProductId(), exception);
        }

        return wishlistMapper.toDTO(entity, prodName, prodImage, price);
    }

    @Transactional(readOnly = true)
    public List<WishlistItemDTO> getAllWishlistItems() {
        return wishlistRepository.findAll(
                org.springframework.data.domain.PageRequest.of(0, 1000))
                .stream().map(this::convertToDTO).collect(Collectors.toList());
    }
}
