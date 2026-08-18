package com.entitykart.monolith.controller;

import com.entitykart.monolith.dto.OrderDTO;
import com.entitykart.monolith.dto.PaymentDTO;
import com.entitykart.monolith.dto.PaymentRequest;
import com.entitykart.monolith.dto.EmiPaymentRequest;
import com.entitykart.monolith.entity.PaymentEntity;
import com.entitykart.monolith.service.PaymentService;
import com.entitykart.monolith.service.OrderService;
import com.entitykart.monolith.mapper.PaymentMapper;
import com.entitykart.monolith.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final OrderService orderService;
    private final PaymentMapper paymentMapper;

    @PostMapping("/process-card")
    public PaymentEntity processCardPayment(@RequestBody PaymentRequest request) {
        return paymentService.processCardPayment(request);
    }

    @PostMapping("/process-offline")
    public PaymentEntity processOfflinePayment(
            @RequestParam Long orderId,
            @RequestParam Double amount,
            @RequestParam String paymentMode,
            @RequestParam(required = false) String customerEmail,
            @RequestParam(required = false) String customerName) {
        return paymentService.processOfflinePayment(orderId, amount, paymentMode, customerEmail, customerName);
    }

    @PostMapping("/process-netbanking")
    public PaymentEntity processNetBankingPayment(
            @RequestParam Long orderId,
            @RequestParam Double amount,
            @RequestParam(required = false, defaultValue = "SBI") String bankName,
            @RequestParam(required = false) String customerEmail,
            @RequestParam(required = false) String customerName) {
        return paymentService.processNetBankingPayment(orderId, amount, bankName, customerEmail, customerName);
    }

    @PostMapping("/process-wallet")
    public PaymentEntity processWalletPayment(
            @RequestParam Long orderId,
            @RequestParam Double amount,
            @RequestParam(required = false, defaultValue = "PAYTM") String walletType,
            @RequestParam(required = false) String customerEmail,
            @RequestParam(required = false) String customerName) {
        return paymentService.processWalletPayment(orderId, amount, walletType, customerEmail, customerName);
    }

    @PostMapping("/process-emi")
    public PaymentEntity processEmiPayment(@RequestBody EmiPaymentRequest req) {
        return paymentService.processEmiPayment(
                req.getOrderId(), req.getAmount(), req.getCardNumber(),
                req.getEmiTenure() != null ? req.getEmiTenure() : 3,
                req.getCustomerEmail(), req.getCustomerName());
    }

    @PostMapping("/assign-cod-transaction/{orderId}")
    public PaymentEntity assignCodTransaction(@PathVariable Long orderId) {
        return paymentService.assignCodTransaction(orderId);
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentDTO> getPaymentByOrder(
            @PathVariable Long orderId,
            @RequestHeader(value = "X-Customer-Id", required = false) Long requestingCustomerId,
            @RequestHeader(value = "X-User-Role", required = false) String requestingRole) {

        if (!"ADMIN".equalsIgnoreCase(requestingRole)) {
            try {
                OrderDTO order = orderService.getOrder(orderId);
                if (order == null || requestingCustomerId == null
                        || !requestingCustomerId.equals(order.getCustomerId())) {
                    log.warn("Blocked payment lookup for orderId={} — requester customerId={} does not own it",
                            orderId, requestingCustomerId);
                    return ResponseEntity.status(403).build();
                }
            } catch (Exception e) {
                log.error("Could not verify order ownership for orderId={}: {}", orderId, e.getMessage());
                return ResponseEntity.status(502).build();
            }
        }

        try {
            PaymentEntity entity = paymentService.getPaymentByOrderId(orderId);
            return ResponseEntity.ok(paymentMapper.toDTO(entity));
        } catch (Exception e) {
            log.info("No payment record yet for orderId={}: {}", orderId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/all")
    public List<PaymentDTO> getAllPayments(
            @RequestHeader(value = "X-User-Role", required = false) String requestingRole) {
        // §4.1 Critical Security Check
        if (!"ADMIN".equalsIgnoreCase(requestingRole)) {
            throw new UnauthorizedException("Admin role required to view all payments");
        }
        return paymentService.getAllPayments().stream()
                .map(paymentMapper::toDTO)
                .collect(Collectors.toList());
    }
}
