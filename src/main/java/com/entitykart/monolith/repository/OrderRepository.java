package com.entitykart.monolith.repository;

import com.entitykart.monolith.entity.OrderEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    Page<OrderEntity> findByCustomerIdOrderByOrderDateDesc(Long customerId, Pageable pageable);

    List<OrderEntity> findByOrderStatus(OrderEntity.OrderStatus status);
}
