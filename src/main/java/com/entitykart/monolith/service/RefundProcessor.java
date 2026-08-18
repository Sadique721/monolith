package com.entitykart.monolith.service;

import com.entitykart.monolith.entity.ReturnEntity;
import com.entitykart.monolith.repository.ReturnRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefundProcessor {

    private final PaymentService paymentService;
    private final ReturnRepository returnRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processRefund(ReturnEntity returnEntity) {
        log.info("Processing refund for returnId={}, amount={}", 
                 returnEntity.getReturnId(), returnEntity.getRefundAmount());
        try {
            paymentService.processOfflinePayment(
                    returnEntity.getOrderId(),
                    returnEntity.getRefundAmount(),
                    "REFUND"
            );
            returnEntity.setStatus(ReturnEntity.ReturnStatus.REFUNDED);
            returnRepository.save(returnEntity);
            log.info("Refund SUCCESS for returnId={}, orderId={}", 
                     returnEntity.getReturnId(), returnEntity.getOrderId());
        } catch (Exception e) {
            log.error("Refund FAILED for returnId={}: {}. Status remains APPROVED for manual retry.",
                      returnEntity.getReturnId(), e.getMessage());
        }
    }
}
