package com.entitykart.monolith.mapper;

import com.entitykart.monolith.dto.PaymentDTO;
import com.entitykart.monolith.entity.PaymentEntity;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public PaymentDTO toDTO(PaymentEntity entity) {
        if (entity == null) {
            return null;
        }
        PaymentDTO dto = new PaymentDTO();
        dto.setPaymentId(entity.getPaymentId());
        dto.setOrderId(entity.getOrderId());
        dto.setAmount(entity.getAmount());
        dto.setPaymentMode(entity.getPaymentMode() != null ? entity.getPaymentMode().name() : "");
        dto.setTransactionRef(entity.getTransactionRef());
        dto.setPaymentStatus(entity.getPaymentStatus() != null ? entity.getPaymentStatus().name() : "");
        dto.setPaymentDate(entity.getPaymentDate());
        return dto;
    }

    public PaymentEntity toEntity(PaymentDTO dto) {
        if (dto == null) {
            return null;
        }
        PaymentEntity entity = new PaymentEntity();
        entity.setPaymentId(dto.getPaymentId());
        entity.setOrderId(dto.getOrderId());
        entity.setAmount(dto.getAmount());
        if (dto.getPaymentMode() != null && !dto.getPaymentMode().isEmpty()) {
            entity.setPaymentMode(PaymentEntity.PaymentMode.valueOf(dto.getPaymentMode().toUpperCase()));
        }
        entity.setTransactionRef(dto.getTransactionRef());
        if (dto.getPaymentStatus() != null && !dto.getPaymentStatus().isEmpty()) {
            entity.setPaymentStatus(PaymentEntity.PaymentStatus.valueOf(dto.getPaymentStatus().toUpperCase()));
        }
        entity.setPaymentDate(dto.getPaymentDate());
        return entity;
    }
}
