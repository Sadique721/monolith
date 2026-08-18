package com.entitykart.monolith.service;

import com.entitykart.monolith.dto.CartItemDTO;
import com.entitykart.monolith.dto.CheckoutRequest;
import com.entitykart.monolith.dto.OrderResponse;
import com.entitykart.monolith.dto.ProductDTO;
import com.entitykart.monolith.entity.CartItemEntity;
import com.entitykart.monolith.repository.CartRepository;
import com.entitykart.monolith.mapper.CartMapper;
import com.entitykart.monolith.event.CartCheckoutEvent;
import com.entitykart.monolith.dto.CouponValidationResponse;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private final CartRepository cartRepository;
    private final ProductService productService;
    private final OrderService orderService;
    private final CartMapper cartMapper;

    @Transactional
    public void addToCart(Long customerId, Long productId, Integer quantity, Double price) {
        validateQuantity(quantity);
        if (quantity > 100) {
            throw new RuntimeException("Cannot add more than 100 units of a product to cart at once");
        }

        Double actualPrice;
        try {
            ProductDTO product = productService.getProduct(productId);
            if (product == null) {
                throw new RuntimeException("Product not found");
            }
            if ("INACTIVE".equalsIgnoreCase(product.getStatus())) {
                throw new RuntimeException("Product is currently unavailable");
            }
            if (product.getStockQuantity() != null && product.getStockQuantity() < quantity) {
                throw new RuntimeException("Insufficient stock. Only " + product.getStockQuantity() + " items available.");
            }
            if (product.getPrice() == null) {
                throw new RuntimeException("Product price is not configured");
            }
            actualPrice = product.getPrice().doubleValue();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Could not validate product info: {}", e.getMessage());
            throw new RuntimeException("Product validation failed: " + e.getMessage());
        }

        validatePrice(actualPrice);

        CartItemEntity existing = cartRepository.findByCustomerIdAndProductId(customerId, productId).orElse(null);
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + quantity);
            cartRepository.save(existing);
        } else {
            CartItemEntity item = new CartItemEntity();
            item.setCustomerId(customerId);
            item.setProductId(productId);
            item.setQuantity(quantity);
            item.setPrice(actualPrice);
            cartRepository.save(item);
        }

        log.info("Added product {} to cart of customer {}", productId, customerId);
    }

    @Transactional
    public void updateQuantity(Long customerId, Long productId, Integer quantity) {
        CartItemEntity item = cartRepository.findByCustomerIdAndProductId(customerId, productId)
                .orElseThrow(() -> new RuntimeException("Item not in cart"));

        if (quantity == null || quantity <= 0) {
            cartRepository.delete(item);
        } else {
            if (quantity > 100) {
                throw new RuntimeException("Cannot exceed 100 units of a product in cart");
            }
            item.setQuantity(quantity);
            cartRepository.save(item);
        }
    }

    @Transactional
    public void removeItem(Long customerId, Long productId) {
        cartRepository.deleteByCustomerIdAndProductId(customerId, productId);
    }

    @Transactional
    public void clearCart(Long customerId) {
        cartRepository.deleteByCustomerId(customerId);
    }

    @Transactional(readOnly = true)
    public List<CartItemDTO> getCartItems(Long customerId) {
        List<CartItemEntity> items = cartRepository.findByCustomerId(customerId);
        if (items.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        List<Long> productIds = items.stream()
                .map(CartItemEntity::getProductId)
                .distinct()
                .collect(Collectors.toList());

        java.util.Map<Long, ProductDTO> productMap = new java.util.HashMap<>();
        try {
            List<ProductDTO> productInfos = productService.getProductsByIds(productIds);
            if (productInfos != null) {
                for (ProductDTO info : productInfos) {
                    if (info != null && info.getProductId() != null) {
                        productMap.put(info.getProductId(), info);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not load product details in batch: {}", e.getMessage());
        }

        return items.stream()
                .map(item -> {
                    ProductDTO info = productMap.get(item.getProductId());
                    String name = info != null ? info.getProductName() : "Product " + item.getProductId();
                    String imgUrl = info != null ? info.getMainImageURL() : null;
                    return cartMapper.toDTO(item, name, imgUrl);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Double getCartTotal(Long customerId) {
        Double total = cartRepository.getCartTotal(customerId);
        return total != null ? total : 0.0;
    }

    @Transactional
    public OrderResponse checkout(CheckoutRequest request) {
        Long customerId = request.getCustomerId();
        List<CartItemDTO> items = getCartItems(customerId);
        if (items.isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        Double total = getCartTotal(customerId);
        
        List<CartItemDTO> sharedItems = items.stream()
                .map(item -> {
                    CartItemDTO d = new CartItemDTO();
                    d.setProductId(item.getProductId());
                    d.setQuantity(item.getQuantity());
                    d.setPrice(item.getPrice());
                    return d;
                })
                .collect(Collectors.toList());

        CartCheckoutEvent event = new CartCheckoutEvent(
                customerId,
                request.getAddressId(),
                sharedItems,
                total,
                request.getPaymentMode()
        );

        com.entitykart.monolith.dto.OrderDTO orderDTO = orderService.createOrder(event);
        clearCart(customerId);

        log.info("Synchronous order creation completed for customer {} -> orderId: {}", customerId, orderDTO.getOrderId());
        
        OrderResponse orderResponse = new OrderResponse();
        orderResponse.setOrderId(orderDTO.getOrderId());
        orderResponse.setCustomerId(orderDTO.getCustomerId());
        orderResponse.setAddressId(orderDTO.getAddressId());
        orderResponse.setTotalAmount(orderDTO.getTotalAmount());
        orderResponse.setOrderStatus(orderDTO.getOrderStatus());
        orderResponse.setPaymentStatus(orderDTO.getPaymentStatus());
        orderResponse.setOrderDate(orderDTO.getOrderDate());
        return orderResponse;
    }

    private void validateQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new RuntimeException("Quantity must be greater than zero");
        }
    }

    private void validatePrice(Double price) {
        if (price == null || price < 0) {
            throw new RuntimeException("Price must be zero or greater");
        }
    }

    public CouponValidationResponse validateCoupon(String code, Double cartTotal) {
        if (code == null || code.isBlank()) {
            return new CouponValidationResponse(
                    false, code, null, null, null, "Invalid coupon code");
        }

        String upper = code.trim().toUpperCase();

        return switch (upper) {
            case "SAVE10" -> new CouponValidationResponse(
                    true, upper, "PERCENT", 10.0, 500.0, "10% off (max ₹500)");
            case "FLAT100" -> {
                if (cartTotal < 500) {
                    yield new CouponValidationResponse(
                            false, upper, "FIXED", 100.0, null, "Minimum order ₹500 required");
                }
                yield new CouponValidationResponse(
                        true, upper, "FIXED", 100.0, null, "Flat ₹100 off");
            }
            case "ENTITYKART20" -> {
                if (cartTotal < 1000) {
                    yield new CouponValidationResponse(
                            false, upper, "PERCENT", 20.0, 1000.0, "Minimum order ₹1000 required");
                }
                yield new CouponValidationResponse(
                        true, upper, "PERCENT", 20.0, 1000.0, "20% off (max ₹1000)");
            }
            default -> new CouponValidationResponse(
                    false, code, null, null, null, "Coupon not found or expired");
        };
    }
}
