package com.entitykart.monolith.service;

import com.entitykart.monolith.dto.OrderDTO;
import com.entitykart.monolith.dto.UserDTO;
import com.entitykart.monolith.dto.ReturnRequest;
import com.entitykart.monolith.dto.ReturnResponse;
import com.entitykart.monolith.dto.AdminDecisionRequest;
import com.entitykart.monolith.entity.ReturnEntity;
import com.entitykart.monolith.repository.ReturnRepository;
import com.entitykart.monolith.event.ReturnApprovedEvent;
import com.entitykart.monolith.event.ReturnRejectedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReturnService {

    private final ReturnRepository returnRepository;
    private final OrderService orderService;
    private final UserService userService;
    private final RefundProcessor refundProcessor;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ReturnResponse createReturn(Long customerId, ReturnRequest request) {
        OrderDTO order = orderService.getOrder(request.getOrderId());

        if (!order.getCustomerId().equals(customerId)) {
            throw new RuntimeException("Order does not belong to this customer");
        }
        if (!"DELIVERED".equalsIgnoreCase(order.getOrderStatus())) {
            throw new RuntimeException("Return can only be requested for delivered orders. Current status: " + order.getOrderStatus());
        }

        if (order.getOrderDate() != null) {
            LocalDate orderDate = order.getOrderDate().toLocalDate();
            if (LocalDate.now().isAfter(orderDate.plusDays(30))) {
                throw new RuntimeException("Return period expired. Returns are only allowed within 30 days of placing the order.");
            }
        }

        boolean duplicateExists = returnRepository.existsByOrderIdAndProductIdAndStatusNot(
                request.getOrderId(), request.getProductId(), ReturnEntity.ReturnStatus.REJECTED);
        if (duplicateExists) {
            throw new RuntimeException("A return request already exists for this product in the order");
        }

        OrderDTO.OrderItemDTO matchedItem = order.getItems().stream()
                .filter(item -> item.getProductId().equals(request.getProductId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Product not found in the specified order"));

        if (request.getQuantity() > matchedItem.getQuantity()) {
            throw new RuntimeException("Return quantity (" + request.getQuantity()
                    + ") exceeds ordered quantity (" + matchedItem.getQuantity() + ")");
        }

        double refundAmount = matchedItem.getPrice() * request.getQuantity();

        ReturnEntity entity = new ReturnEntity();
        entity.setOrderId(request.getOrderId());
        entity.setCustomerId(customerId);
        entity.setProductId(request.getProductId());
        entity.setQuantity(request.getQuantity());
        entity.setReason(request.getReason());
        entity.setStatus(ReturnEntity.ReturnStatus.PENDING);
        entity.setRefundAmount(refundAmount);

        ReturnEntity saved = returnRepository.save(entity);
        log.info("Return request created: returnId={}, orderId={}, customerId={}", saved.getReturnId(), saved.getOrderId(), customerId);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ReturnResponse> getReturnsByCustomer(Long customerId) {
        return returnRepository.findByCustomerId(customerId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ReturnResponse getReturnById(Long returnId, Long customerId) {
        ReturnEntity entity = returnRepository.findById(returnId)
                .orElseThrow(() -> new RuntimeException("Return request not found: " + returnId));
        if (!entity.getCustomerId().equals(customerId)) {
            throw new RuntimeException("Access denied: this return does not belong to you");
        }
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<ReturnResponse> getAllReturns() {
        return returnRepository.findAll(
                org.springframework.data.domain.PageRequest.of(0, 500))
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReturnResponse> getReturnsByStatus(String status) {
        ReturnEntity.ReturnStatus returnStatus = ReturnEntity.ReturnStatus.valueOf(status.toUpperCase());
        return returnRepository.findByStatus(returnStatus)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public ReturnResponse processAdminDecision(Long returnId, AdminDecisionRequest decision) {
        ReturnEntity entity = returnRepository.findById(returnId)
                .orElseThrow(() -> new RuntimeException("Return request not found: " + returnId));

        if (entity.getStatus() != ReturnEntity.ReturnStatus.PENDING) {
            throw new RuntimeException("Only PENDING returns can be approved or rejected. Current status: " + entity.getStatus());
        }

        String dec = decision.getDecision().toUpperCase();

        if ("APPROVED".equals(dec)) {
            if (decision.getRefundAmount() != null && decision.getRefundAmount() > 0) {
                entity.setRefundAmount(decision.getRefundAmount());
            }
            entity.setStatus(ReturnEntity.ReturnStatus.APPROVED);
            entity.setAdminNote(decision.getAdminNote());

            try {
                orderService.updateOrderStatus(entity.getOrderId(), "RETURNED");
            } catch (Exception e) {
                log.warn("Could not update order status for orderId={}: {}", entity.getOrderId(), e.getMessage());
            }

            refundProcessor.processRefund(entity);

        } else if ("REJECTED".equals(dec)) {
            entity.setStatus(ReturnEntity.ReturnStatus.REJECTED);
            entity.setAdminNote(decision.getAdminNote());
            entity.setRejectionReason(decision.getRejectionReason());
            log.info("Return {} rejected. Reason: {}", returnId, decision.getRejectionReason());
        } else {
            throw new RuntimeException("Invalid decision. Must be APPROVED or REJECTED");
        }

        ReturnEntity saved = returnRepository.save(entity);
        publishReturnEvent(saved);
        return toResponse(saved);
    }

    @Transactional
    public ReturnResponse processManualRefund(Long returnId) {
        ReturnEntity entity = returnRepository.findById(returnId)
                .orElseThrow(() -> new RuntimeException("Return request not found: " + returnId));
        if (entity.getStatus() != ReturnEntity.ReturnStatus.APPROVED) {
            throw new RuntimeException("Refund can only be processed for APPROVED returns. Current status: " + entity.getStatus());
        }
        refundProcessor.processRefund(entity);
        return toResponse(entity);
    }

    private void publishReturnEvent(ReturnEntity entity) {
        String customerEmail = null;
        String customerName  = "Customer";
        try {
            UserDTO userInfo = userService.getUserById(entity.getCustomerId());
            if (userInfo != null) {
                customerEmail = userInfo.getEmail();
                customerName  = userInfo.getName() != null ? userInfo.getName() : "Customer";
            }
        } catch (Exception e) {
            log.warn("Could not fetch customer details for returnId={}, email will be missing: {}",
                     entity.getReturnId(), e.getMessage());
        }

        Object event;
        if (entity.getStatus() == ReturnEntity.ReturnStatus.APPROVED) {
            event = new ReturnApprovedEvent(
                    entity.getReturnId(),
                    entity.getOrderId(),
                    entity.getCustomerId(),
                    entity.getProductId(),
                    entity.getRefundAmount(),
                    entity.getStatus().name(),
                    customerEmail,
                    customerName
            );
        } else if (entity.getStatus() == ReturnEntity.ReturnStatus.REJECTED) {
            event = new ReturnRejectedEvent(
                    entity.getReturnId(),
                    entity.getOrderId(),
                    entity.getCustomerId(),
                    entity.getProductId(),
                    entity.getRejectionReason(),
                    entity.getStatus().name(),
                    customerEmail,
                    customerName
            );
        } else {
            log.warn("Skipping return event publication for status: {}", entity.getStatus());
            return;
        }

        try {
            eventPublisher.publishEvent(event);
            log.info("Published return event successfully via EventPublisher for returnId={}, status={}", entity.getReturnId(), entity.getStatus());
        } catch (Exception e) {
            log.error("Failed to publish return event: {}", e.getMessage());
        }
    }

    private ReturnResponse toResponse(ReturnEntity entity) {
        ReturnResponse response = new ReturnResponse();
        response.setReturnId(entity.getReturnId());
        response.setOrderId(entity.getOrderId());
        response.setCustomerId(entity.getCustomerId());
        response.setProductId(entity.getProductId());
        response.setQuantity(entity.getQuantity());
        response.setReason(entity.getReason());
        response.setStatus(entity.getStatus().name());
        response.setRefundAmount(entity.getRefundAmount());
        response.setAdminNote(entity.getAdminNote());
        response.setRejectionReason(entity.getRejectionReason());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }
}
