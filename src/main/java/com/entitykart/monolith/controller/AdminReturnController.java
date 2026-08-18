package com.entitykart.monolith.controller;

import com.entitykart.monolith.dto.AdminDecisionRequest;
import com.entitykart.monolith.dto.ReturnResponse;
import com.entitykart.monolith.service.ReturnService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/returns")
@RequiredArgsConstructor
public class AdminReturnController {

    private final ReturnService returnService;

    @GetMapping
    public ResponseEntity<List<ReturnResponse>> getAllReturns(
            @RequestParam(required = false) String status) {
        if (status != null && !status.isBlank()) {
            return ResponseEntity.ok(returnService.getReturnsByStatus(status));
        }
        return ResponseEntity.ok(returnService.getAllReturns());
    }

    @GetMapping("/{returnId}")
    public ResponseEntity<ReturnResponse> getReturn(@PathVariable Long returnId) {
        return ResponseEntity.ok(returnService.getAllReturns().stream()
                .filter(r -> r.getReturnId().equals(returnId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Return not found: " + returnId)));
    }

    @PatchMapping("/{returnId}/decision")
    public ResponseEntity<ReturnResponse> processDecision(
            @PathVariable Long returnId,
            @Valid @RequestBody AdminDecisionRequest decisionRequest) {
        return ResponseEntity.ok(returnService.processAdminDecision(returnId, decisionRequest));
    }

    @PostMapping("/{returnId}/refund")
    public ResponseEntity<ReturnResponse> processRefund(@PathVariable Long returnId) {
        return ResponseEntity.ok(returnService.processManualRefund(returnId));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<ReturnResponse>> getReturnsByOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(returnService.getAllReturns().stream()
                .filter(r -> r.getOrderId().equals(orderId))
                .toList());
    }
}
