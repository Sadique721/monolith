package com.entitykart.monolith.service;

import com.entitykart.monolith.dto.OrderDTO;
import com.entitykart.monolith.dto.UserDTO;
import com.entitykart.monolith.entity.OrderEntity;
import com.entitykart.monolith.entity.OrderItemEntity;
import com.entitykart.monolith.event.OrderPlacedEvent;
import com.entitykart.monolith.repository.OrderItemRepository;
import com.entitykart.monolith.repository.OrderRepository;
import com.entitykart.monolith.event.CartCheckoutEvent;
import com.entitykart.monolith.mapper.OrderMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserService userService;
    private final OrderMapper orderMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public OrderDTO getOrder(Long orderId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        List<OrderItemEntity> items = orderItemRepository.findByOrderId(order.getOrderId());
        return orderMapper.toDTO(order, items);
    }

    @Transactional(readOnly = true)
    public Page<OrderDTO> getOrdersByCustomer(Long customerId, Pageable pageable) {
        return orderRepository.findByCustomerIdOrderByOrderDateDesc(customerId, pageable)
                .map(order -> {
                    List<OrderItemEntity> items = orderItemRepository.findByOrderId(order.getOrderId());
                    return orderMapper.toDTO(order, items);
                });
    }

    @Transactional(readOnly = true)
    public Page<OrderDTO> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable)
                .map(order -> {
                    List<OrderItemEntity> items = orderItemRepository.findByOrderId(order.getOrderId());
                    return orderMapper.toDTO(order, items);
                });
    }

    @Transactional
    public OrderDTO createOrder(CartCheckoutEvent event) {
        log.info("Creating order synchronously for customer: {} with paymentMode: {}", event.getCustomerId(), event.getPaymentMode());

        OrderEntity order = new OrderEntity();
        order.setCustomerId(event.getCustomerId());
        order.setAddressId(event.getAddressId());
        order.setTotalAmount(event.getTotalAmount());
        order.setOrderStatus(OrderEntity.OrderStatus.PENDING_PAYMENT);
        order.setPaymentStatus(OrderEntity.PaymentStatus.UNPAID);
        order.setOrderDate(LocalDateTime.now());
        OrderEntity savedOrder = orderRepository.save(order);

        List<OrderItemEntity> orderItems = event.getItems().stream().map(item -> {
            OrderItemEntity orderItem = new OrderItemEntity();
            orderItem.setOrderId(savedOrder.getOrderId());
            orderItem.setProductId(item.getProductId());
            orderItem.setQuantity(item.getQuantity());
            orderItem.setPrice(item.getPrice());
            return orderItem;
        }).collect(Collectors.toList());

        orderItemRepository.saveAll(orderItems);

        UserDTO userInfo = null;
        try {
            userInfo = userService.getUserById(event.getCustomerId());
        } catch (Exception e) {
            log.error("Could not fetch customerId={} from UserService — order-placed email will be SKIPPED: {}",
                    event.getCustomerId(), e.getMessage());
        }

        if (userInfo == null || userInfo.getEmail() == null || userInfo.getEmail().isBlank()) {
            log.error("No valid email resolved for customerId={} — NOT publishing order-events for orderId={}",
                    event.getCustomerId(), savedOrder.getOrderId());
        } else {
            OrderPlacedEvent placedEvent = new OrderPlacedEvent(
                    savedOrder.getOrderId(),
                    savedOrder.getCustomerId(),
                    savedOrder.getTotalAmount(),
                    LocalDateTime.now(),
                    userInfo.getEmail(),
                    userInfo.getName() != null ? userInfo.getName() : "Customer",
                    savedOrder.getOrderStatus().name(),
                    event.getPaymentMode(),
                    null
            );

            try {
                eventPublisher.publishEvent(placedEvent);
                log.info("Order placed event published via ApplicationEventPublisher for orderId={} to email={}", savedOrder.getOrderId(), userInfo.getEmail());
            } catch (Exception e) {
                log.error("Failed to publish order-events for orderId={}: {}", savedOrder.getOrderId(), e.getMessage());
            }
        }

        return orderMapper.toDTO(savedOrder, orderItems);
    }

    @Transactional
    public void updateOrderStatus(Long orderId, String status) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (isPaymentStatus(status)) {
            OrderEntity.PaymentStatus paymentStatus = OrderEntity.PaymentStatus.valueOf(status.toUpperCase());
            order.setPaymentStatus(paymentStatus);
            if (paymentStatus == OrderEntity.PaymentStatus.PAID
                    && order.getOrderStatus() == OrderEntity.OrderStatus.PENDING_PAYMENT) {
                order.setOrderStatus(OrderEntity.OrderStatus.PLACED);
            }
        } else {
            try {
                order.setOrderStatus(OrderEntity.OrderStatus.valueOf(status.toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid order status: " + status);
            }
        }

        orderRepository.save(order);
        publishOrderStatusEvent(order);
    }

    @Transactional
    public void updatePaymentStatus(Long orderId, String paymentStatus) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        OrderEntity.PaymentStatus ps = OrderEntity.PaymentStatus.valueOf(paymentStatus.toUpperCase());
        order.setPaymentStatus(ps);
        if (ps == OrderEntity.PaymentStatus.PAID
                && order.getOrderStatus() == OrderEntity.OrderStatus.PENDING_PAYMENT) {
            order.setOrderStatus(OrderEntity.OrderStatus.PLACED);
        }
        orderRepository.save(order);
        publishOrderStatusEvent(order);
    }

    private void publishOrderStatusEvent(OrderEntity order) {
        UserDTO userInfo = null;
        try {
            userInfo = userService.getUserById(order.getCustomerId());
        } catch (Exception e) {
            log.error("Could not fetch customerId={} from UserService — status-change email will be SKIPPED: {}",
                    order.getCustomerId(), e.getMessage());
        }

        if (userInfo == null || userInfo.getEmail() == null || userInfo.getEmail().isBlank()) {
            log.error("No valid email resolved for customerId={} — NOT publishing order-events for orderId={}",
                    order.getCustomerId(), order.getOrderId());
            return;
        }

        OrderPlacedEvent event = new OrderPlacedEvent(
                order.getOrderId(),
                order.getCustomerId(),
                order.getTotalAmount(),
                LocalDateTime.now(),
                userInfo.getEmail(),
                userInfo.getName() != null ? userInfo.getName() : "Customer",
                order.getOrderStatus().name(),
                null,
                null
        );

        try {
            eventPublisher.publishEvent(event);
            log.info("Published order status event: orderId={}, status={}, email={}",
                    order.getOrderId(), order.getOrderStatus(), userInfo.getEmail());
        } catch (Exception e) {
            log.error("Failed to publish order status event for orderId={}: {}", order.getOrderId(), e.getMessage());
        }
    }

    private boolean isPaymentStatus(String status) {
        try {
            OrderEntity.PaymentStatus.valueOf(status.toUpperCase());
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    @Transactional(readOnly = true)
    public boolean hasCustomerPurchasedProduct(Long customerId, Long productId) {
        // Fetch all orders of customer
        List<OrderEntity> orders = orderRepository.findByCustomerIdOrderByOrderDateDesc(customerId, Pageable.unpaged()).getContent();
        for (OrderEntity order : orders) {
            if (order.getOrderStatus() == OrderEntity.OrderStatus.DELIVERED) {
                List<OrderItemEntity> items = orderItemRepository.findByOrderId(order.getOrderId());
                for (OrderItemEntity item : items) {
                    if (item.getProductId().equals(productId)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
