package com.entitykart.monolith.listener;

import com.entitykart.monolith.event.OrderPlacedEvent;
import com.entitykart.monolith.event.PaymentProcessedEvent;
import com.entitykart.monolith.event.ReturnApprovedEvent;
import com.entitykart.monolith.event.ReturnRejectedEvent;
import com.entitykart.monolith.event.UserCreatedEvent;
import com.entitykart.monolith.event.PasswordResetEvent;
import com.entitykart.monolith.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApplicationNotificationListener {

    private final NotificationService notificationService;

    @Async
    @EventListener
    public void onOrderEvent(OrderPlacedEvent event) {
        log.info("Received order event via ApplicationEvent: orderId={}, status={}", event.getOrderId(), event.getOrderStatus());
        try {
            if (event.getCustomerEmail() == null || event.getCustomerEmail().isBlank()) {
                log.warn("Order event missing customerEmail, skipping for orderId={}", event.getOrderId());
                return;
            }
            String status = event.getOrderStatus() != null ? event.getOrderStatus().toUpperCase() : "";
            switch (status) {
                case "PLACED":
                case "PENDING_PAYMENT":
                    notificationService.handleOrderPlaced(
                            event.getOrderId(),
                            event.getCustomerId(),
                            event.getCustomerEmail(),
                            event.getCustomerName() != null ? event.getCustomerName() : "Customer",
                            event.getTotalAmount()
                    );
                    break;
                case "CONFIRMED":
                    notificationService.handleOrderConfirmed(
                            event.getOrderId(), event.getCustomerId(),
                            event.getCustomerEmail(),
                            event.getCustomerName() != null ? event.getCustomerName() : "Customer",
                            event.getTotalAmount());
                    break;
                case "SHIPPED":
                    notificationService.handleOrderShipped(
                            event.getOrderId(), event.getCustomerId(),
                            event.getCustomerEmail(),
                            event.getCustomerName() != null ? event.getCustomerName() : "Customer",
                            event.getTotalAmount());
                    break;
                case "DELIVERED":
                    notificationService.handleOrderDelivered(
                            event.getOrderId(), event.getCustomerId(),
                            event.getCustomerEmail(),
                            event.getCustomerName() != null ? event.getCustomerName() : "Customer",
                            event.getTotalAmount());
                    break;
                case "CANCELLED":
                    notificationService.handleOrderCancelled(
                            event.getOrderId(), event.getCustomerId(),
                            event.getCustomerEmail(),
                            event.getCustomerName() != null ? event.getCustomerName() : "Customer",
                            event.getTotalAmount());
                    break;
                case "RETURNED":
                    notificationService.handleOrderReturned(
                            event.getOrderId(), event.getCustomerId(),
                            event.getCustomerEmail(),
                            event.getCustomerName() != null ? event.getCustomerName() : "Customer",
                            event.getTotalAmount());
                    break;
                default:
                    log.info("No notification configured for order status: {}", status);
            }
        } catch (Exception e) {
            log.error("Failed to process order event for orderId={}: {}", event.getOrderId(), e.getMessage());
        }
    }

    @Async
    @EventListener
    public void onPaymentEvent(PaymentProcessedEvent event) {
        log.info("Received payment event via ApplicationEvent: orderId={}, status={}", event.getOrderId(), event.getStatus());
        try {
            if (event.getCustomerEmail() == null || event.getCustomerEmail().isBlank()) {
                log.warn("Payment event missing customerEmail, skipping for orderId={}", event.getOrderId());
                return;
            }
            String name = event.getCustomerName() != null ? event.getCustomerName() : "Customer";
            if ("SUCCESS".equalsIgnoreCase(event.getStatus())) {
                notificationService.handlePaymentSuccess(
                        event.getOrderId(),
                        null,
                        event.getCustomerEmail(),
                        name,
                        event.getTransactionRef(),
                        event.getAmount()
                );
            } else {
                notificationService.handlePaymentFailed(
                        event.getOrderId(), null, event.getCustomerEmail(), name);
            }
        } catch (Exception e) {
            log.error("Failed to process payment event for orderId={}: {}", event.getOrderId(), e.getMessage());
        }
    }

    @Async
    @EventListener
    public void onReturnApprovedEvent(ReturnApprovedEvent event) {
        log.info("Received return approved event via ApplicationEvent: returnId={}, status={}", event.getReturnId(), event.getStatus());
        try {
            if (event.getCustomerEmail() == null || event.getCustomerEmail().isBlank()) {
                log.warn("Return approved event missing customerEmail, skipping for returnId={}", event.getReturnId());
                return;
            }
            notificationService.handleReturnStatusUpdate(
                    event.getReturnId(),
                    event.getCustomerId(),
                    event.getCustomerEmail(),
                    event.getCustomerName() != null ? event.getCustomerName() : "Customer",
                    event.getStatus(),
                    event.getRefundAmount(),
                    null
            );
        } catch (Exception e) {
            log.error("Failed to process return approved event for returnId={}: {}", event.getReturnId(), e.getMessage());
        }
    }

    @Async
    @EventListener
    public void onReturnRejectedEvent(ReturnRejectedEvent event) {
        log.info("Received return rejected event via ApplicationEvent: returnId={}, status={}", event.getReturnId(), event.getStatus());
        try {
            if (event.getCustomerEmail() == null || event.getCustomerEmail().isBlank()) {
                log.warn("Return rejected event missing customerEmail, skipping for returnId={}", event.getReturnId());
                return;
            }
            notificationService.handleReturnStatusUpdate(
                    event.getReturnId(),
                    event.getCustomerId(),
                    event.getCustomerEmail(),
                    event.getCustomerName() != null ? event.getCustomerName() : "Customer",
                    event.getStatus(),
                    0.0,
                    event.getReason()
            );
        } catch (Exception e) {
            log.error("Failed to process return rejected event for returnId={}: {}", event.getReturnId(), e.getMessage());
        }
    }

    @Async
    @EventListener
    public void onUserCreatedEvent(UserCreatedEvent event) {
        log.info("Received user created event via ApplicationEvent: id={}, email={}", event.getId(), event.getEmail());
        try {
            if (event.getEmail() == null || event.getEmail().isBlank()) {
                log.warn("User created event missing email, skipping");
                return;
            }
            notificationService.handleWelcome(
                    event.getId(),
                    event.getEmail(),
                    event.getName() != null ? event.getName() : "Customer"
            );
        } catch (Exception e) {
            log.error("Failed to process user created event: {}", e.getMessage());
        }
    }

    @Async
    @EventListener
    public void onPasswordResetEvent(PasswordResetEvent event) {
        log.info("Received password reset event via ApplicationEvent: id={}, email={}", event.getId(), event.getEmail());
        try {
            if (event.getEmail() == null || event.getEmail().isBlank()) {
                log.warn("Password reset event missing email, skipping");
                return;
            }
            notificationService.handlePasswordReset(
                    event.getId(),
                    event.getEmail(),
                    event.getName() != null ? event.getName() : "Customer",
                    event.getToken()
            );
        } catch (Exception e) {
            log.error("Failed to process password reset event: {}", e.getMessage());
        }
    }
}
