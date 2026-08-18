package com.entitykart.monolith.service;

import com.entitykart.monolith.dto.OrderDTO;
import com.entitykart.monolith.dto.UserDTO;
import com.entitykart.monolith.event.PaymentProcessedEvent;
import com.entitykart.monolith.dto.PaymentRequest;
import com.entitykart.monolith.entity.PaymentEntity;
import com.entitykart.monolith.repository.PaymentRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.authorize.Environment;
import net.authorize.api.contract.v1.CreateTransactionRequest;
import net.authorize.api.contract.v1.CreateTransactionResponse;
import net.authorize.api.contract.v1.CreditCardType;
import net.authorize.api.contract.v1.MerchantAuthenticationType;
import net.authorize.api.contract.v1.MessageTypeEnum;
import net.authorize.api.contract.v1.PaymentType;
import net.authorize.api.contract.v1.TransactionRequestType;
import net.authorize.api.contract.v1.TransactionResponse;
import net.authorize.api.contract.v1.TransactionTypeEnum;
import net.authorize.api.controller.CreateTransactionController;
import net.authorize.api.controller.base.ApiOperationBase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderService orderService;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${authorize.net.api-login-id}")
    private String apiLoginId;

    @Value("${authorize.net.transaction-key}")
    private String transactionKey;

    @Value("${authorize.net.environment:sandbox}")
    private String environment;

    @Transactional
    public PaymentEntity checkAndCreateInitialPayment(PaymentRequest request) {
        paymentRepository.findByOrderId(request.getOrderId())
                .filter(p -> p.getPaymentStatus() == PaymentEntity.PaymentStatus.SUCCESS)
                .ifPresent(p -> {
                    throw new RuntimeException("Payment has already been successfully processed for order " + request.getOrderId());
                });

        PaymentEntity payment = paymentRepository.findByOrderId(request.getOrderId()).orElse(null);
        if (payment == null) {
            payment = new PaymentEntity();
            payment.setOrderId(request.getOrderId());
            payment.setAmount(request.getAmount());
            payment.setPaymentMode(PaymentEntity.PaymentMode.CARD);
            payment.setPaymentStatus(PaymentEntity.PaymentStatus.PENDING);
            payment = paymentRepository.save(payment);
        }
        return payment;
    }

    @Transactional
    public PaymentEntity updatePaymentStatusAndPublish(Long paymentId, PaymentEntity.PaymentStatus status, String transId, String responseText, String customerEmail, String customerName) {
        PaymentEntity payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment record not found: " + paymentId));
        payment.setPaymentStatus(status);
        payment.setGatewayTransactionId(transId);
        payment.setTransactionRef(transId != null ? transId : "REF_" + System.currentTimeMillis());
        payment.setPaymentDate(LocalDateTime.now());
        payment.setGatewayResponseText(responseText);
        
        return saveAndPublish(payment, customerEmail, customerName);
    }

    public PaymentEntity processCardPayment(PaymentRequest request) {
        boolean isMockMode = "test".equalsIgnoreCase(environment)
                || "mock".equalsIgnoreCase(environment)
                || apiLoginId == null
                || apiLoginId.contains("dummy");

        PaymentEntity payment = checkAndCreateInitialPayment(request);
        if (payment.getPaymentStatus() == PaymentEntity.PaymentStatus.SUCCESS) {
            return payment;
        }

        if (isMockMode) {
            return updatePaymentStatusAndPublish(payment.getPaymentId(), 
                PaymentEntity.PaymentStatus.SUCCESS, 
                "MOCK_CARD_" + System.currentTimeMillis(), 
                "Simulated Approved", 
                request.getCustomerEmail(), 
                request.getCustomerName());
        }

        configureAuthorizeNet();

        MerchantAuthenticationType merchantAuth = new MerchantAuthenticationType();
        merchantAuth.setName(apiLoginId);
        merchantAuth.setTransactionKey(transactionKey);
        ApiOperationBase.setMerchantAuthentication(merchantAuth);

        CreditCardType creditCard = new CreditCardType();
        creditCard.setCardNumber(request.getCardNumber().replaceAll("\\s", ""));
        creditCard.setExpirationDate(formatExpiry(request.getExpiryMonth(), request.getExpiryYear()));
        creditCard.setCardCode(request.getCvv());

        PaymentType paymentType = new PaymentType();
        paymentType.setCreditCard(creditCard);

        TransactionRequestType txnRequest = new TransactionRequestType();
        txnRequest.setTransactionType(TransactionTypeEnum.AUTH_CAPTURE_TRANSACTION.value());
        txnRequest.setPayment(paymentType);
        txnRequest.setAmount(BigDecimal.valueOf(request.getAmount()));

        CreateTransactionRequest apiRequest = new CreateTransactionRequest();
        apiRequest.setMerchantAuthentication(merchantAuth);
        apiRequest.setTransactionRequest(txnRequest);

        CreateTransactionController controller = new CreateTransactionController(apiRequest);
        controller.execute();

        CreateTransactionResponse response = controller.getApiResponse();

        PaymentEntity.PaymentStatus finalStatus = PaymentEntity.PaymentStatus.FAILED;
        String transId = null;
        String responseText = "Gateway error";

        if (response != null && response.getMessages().getResultCode() == MessageTypeEnum.OK) {
            TransactionResponse result = response.getTransactionResponse();
            if (result != null && result.getMessages() != null) {
                finalStatus = PaymentEntity.PaymentStatus.SUCCESS;
                transId = result.getTransId();
                responseText = result.getMessages().getMessage().get(0).getDescription();
            } else if (result != null && result.getErrors() != null && !result.getErrors().getError().isEmpty()) {
                responseText = result.getErrors().getError().get(0).getErrorText();
            }
        } else {
            responseText = getGatewayFailureMessage(response);
        }

        return updatePaymentStatusAndPublish(payment.getPaymentId(), finalStatus, transId, responseText, request.getCustomerEmail(), request.getCustomerName());
    }

    @Transactional
    public PaymentEntity processOfflinePayment(Long orderId, Double amount, String paymentMode) {
        return processOfflinePayment(orderId, amount, paymentMode, null, null);
    }

    @Transactional
    public PaymentEntity processOfflinePayment(Long orderId, Double amount, String paymentMode,
                                                String customerEmail, String customerName) {
        paymentRepository.findByOrderId(orderId)
                .filter(p -> p.getPaymentStatus() == PaymentEntity.PaymentStatus.SUCCESS)
                .ifPresent(p -> {
                    throw new RuntimeException("Payment has already been successfully processed for order " + orderId);
                });

        PaymentEntity payment = new PaymentEntity();
        payment.setOrderId(orderId);
        payment.setAmount(amount);
        payment.setPaymentMode(PaymentEntity.PaymentMode.valueOf(paymentMode.toUpperCase()));

        String prefix;
        switch (paymentMode.toUpperCase()) {
            case "UPI":         prefix = "UPI_";  break;
            case "NET_BANKING": prefix = "NB_";   break;
            case "WALLET":      prefix = "WLT_";  break;
            case "EMI":         prefix = "EMI_";  break;
            case "COD":         prefix = "COD_PENDING_" + orderId + "_"; break;
            default:            prefix = "OFFLINE_"; break;
        }
        payment.setTransactionRef(prefix + System.currentTimeMillis());

        if ("COD".equals(paymentMode.toUpperCase())) {
            payment.setPaymentStatus(PaymentEntity.PaymentStatus.PENDING);
        } else {
            payment.setPaymentStatus(PaymentEntity.PaymentStatus.SUCCESS);
            payment.setPaymentDate(LocalDateTime.now());
        }

        return saveAndPublish(payment, customerEmail, customerName);
    }

    @Transactional
    public PaymentEntity processNetBankingPayment(Long orderId, Double amount, String bankName,
                                                   String customerEmail, String customerName) {
        paymentRepository.findByOrderId(orderId)
                .filter(p -> p.getPaymentStatus() == PaymentEntity.PaymentStatus.SUCCESS)
                .ifPresent(p -> {
                    throw new RuntimeException("Payment has already been successfully processed for order " + orderId);
                });

        PaymentEntity payment = new PaymentEntity();
        payment.setOrderId(orderId);
        payment.setAmount(amount);
        payment.setPaymentMode(PaymentEntity.PaymentMode.NET_BANKING);
        payment.setTransactionRef("NB_" + (bankName != null ? bankName.toUpperCase() : "BANK") + "_" + System.currentTimeMillis());
        payment.setPaymentStatus(PaymentEntity.PaymentStatus.SUCCESS);
        payment.setPaymentDate(LocalDateTime.now());
        payment.setGatewayResponseText("Net Banking via " + bankName + " — Simulated Success");
        log.info("Net Banking payment for order {} via bank {}", orderId, bankName);
        return saveAndPublish(payment, customerEmail, customerName);
    }

    @Transactional
    public PaymentEntity processWalletPayment(Long orderId, Double amount, String walletType,
                                               String customerEmail, String customerName) {
        paymentRepository.findByOrderId(orderId)
                .filter(p -> p.getPaymentStatus() == PaymentEntity.PaymentStatus.SUCCESS)
                .ifPresent(p -> {
                    throw new RuntimeException("Payment has already been successfully processed for order " + orderId);
                });

        PaymentEntity payment = new PaymentEntity();
        payment.setOrderId(orderId);
        payment.setAmount(amount);
        payment.setPaymentMode(PaymentEntity.PaymentMode.WALLET);
        payment.setTransactionRef("WLT_" + (walletType != null ? walletType.toUpperCase() : "WALLET") + "_" + System.currentTimeMillis());
        payment.setPaymentStatus(PaymentEntity.PaymentStatus.SUCCESS);
        payment.setPaymentDate(LocalDateTime.now());
        payment.setGatewayResponseText("Wallet payment via " + walletType + " — Simulated Success");
        log.info("Wallet payment for order {} via {}", orderId, walletType);
        return saveAndPublish(payment, customerEmail, customerName);
    }

    @Transactional
    public PaymentEntity processEmiPayment(Long orderId, Double amount, String cardNumber,
                                            Integer emiTenure, String customerEmail, String customerName) {
        paymentRepository.findByOrderId(orderId)
                .filter(p -> p.getPaymentStatus() == PaymentEntity.PaymentStatus.SUCCESS)
                .ifPresent(p -> {
                    throw new RuntimeException("Payment has already been successfully processed for order " + orderId);
                });

        PaymentEntity payment = new PaymentEntity();
        payment.setOrderId(orderId);
        payment.setAmount(amount);
        payment.setPaymentMode(PaymentEntity.PaymentMode.EMI);
        payment.setTransactionRef("EMI_" + emiTenure + "M_" + System.currentTimeMillis());
        payment.setPaymentStatus(PaymentEntity.PaymentStatus.SUCCESS);
        payment.setPaymentDate(LocalDateTime.now());
        payment.setGatewayResponseText("EMI payment — " + emiTenure + " months — Simulated Success");
        log.info("EMI payment for order {} with {} month tenure", orderId, emiTenure);
        return saveAndPublish(payment, customerEmail, customerName);
    }

    @Transactional
    public PaymentEntity assignCodTransaction(Long orderId) {
        PaymentEntity payment = paymentRepository.findByOrderId(orderId)
                .orElseGet(() -> {
                    PaymentEntity p = new PaymentEntity();
                    p.setOrderId(orderId);
                    p.setPaymentMode(PaymentEntity.PaymentMode.COD);
                    return p;
                });

        payment.setTransactionRef("COD_DELIVERED_" + orderId + "_" + System.currentTimeMillis());
        payment.setPaymentStatus(PaymentEntity.PaymentStatus.SUCCESS);
        payment.setPaymentDate(LocalDateTime.now());
        payment.setGatewayResponseText("COD collected on delivery");

        PaymentEntity saved = paymentRepository.save(payment);
        orderService.updatePaymentStatus(orderId, "PAID");
        log.info("COD transaction assigned for delivered order: {}", orderId);
        return saved;
    }

    @Transactional(readOnly = true)
    public PaymentEntity getPaymentByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
    }

    @Transactional(readOnly = true)
    public List<PaymentEntity> getAllPayments() {
        return paymentRepository.findAll(
                org.springframework.data.domain.PageRequest.of(0, 500))
                .getContent();
    }

    private PaymentEntity saveAndPublish(PaymentEntity payment, String customerEmail, String customerName) {
        PaymentEntity saved = paymentRepository.save(payment);

        String resolvedEmail = customerEmail;
        String resolvedName = customerName;
        try {
            OrderDTO order = orderService.getOrder(saved.getOrderId());
            if (order != null && order.getCustomerId() != null) {
                UserDTO userInfo = userService.getUserById(order.getCustomerId());
                if (userInfo != null && userInfo.getEmail() != null && !userInfo.getEmail().isBlank()) {
                    resolvedEmail = userInfo.getEmail();
                    resolvedName = userInfo.getName() != null ? userInfo.getName() : resolvedName;
                }
            }
        } catch (Exception e) {
            log.warn("Could not resolve customer details: {}, falling back to caller-supplied values", e.getMessage());
        }

        PaymentProcessedEvent event = new PaymentProcessedEvent();
        event.setOrderId(saved.getOrderId());
        event.setStatus(saved.getPaymentStatus() == PaymentEntity.PaymentStatus.SUCCESS ? "SUCCESS" : "FAILED");
        event.setTransactionRef(saved.getTransactionRef());
        event.setCustomerEmail(resolvedEmail);
        event.setCustomerName(resolvedName);
        event.setAmount(saved.getAmount());

        if (saved.getPaymentStatus() == PaymentEntity.PaymentStatus.SUCCESS) {
            orderService.updatePaymentStatus(saved.getOrderId(), "PAID");
        } else if (saved.getPaymentStatus() == PaymentEntity.PaymentStatus.FAILED) {
            orderService.updatePaymentStatus(saved.getOrderId(), "UNPAID");
        }

        try {
            eventPublisher.publishEvent(event);
            log.info("Published payment-events successfully for order: {}", saved.getOrderId());
        } catch (Exception e) {
            log.error("Failed to publish payment-events: {}", e.getMessage());
        }
        
        return saved;
    }

    private void configureAuthorizeNet() {
        if ("production".equalsIgnoreCase(environment)) {
            ApiOperationBase.setEnvironment(Environment.PRODUCTION);
        } else {
            ApiOperationBase.setEnvironment(Environment.SANDBOX);
        }
    }

    private String getGatewayFailureMessage(CreateTransactionResponse response) {
        if (response != null && response.getMessages() != null && !response.getMessages().getMessage().isEmpty()) {
            return response.getMessages().getMessage().get(0).getText();
        }
        return "Gateway error";
    }

    private String formatExpiry(String expiryMonth, String expiryYear) {
        if (expiryMonth == null || expiryYear == null) {
            return "2029-12";
        }
        String month = expiryMonth.length() == 1 ? "0" + expiryMonth : expiryMonth;
        String year = expiryYear.length() == 2 ? "20" + expiryYear : expiryYear;
        return year + "-" + month;
    }
}
