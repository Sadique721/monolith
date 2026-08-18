package com.entitykart.monolith.controller;

import com.entitykart.monolith.dto.ReturnRequest;
import com.entitykart.monolith.dto.ReturnResponse;
import com.entitykart.monolith.service.ReturnService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/returns")
@RequiredArgsConstructor
public class ReturnController {

    private final ReturnService returnService;

    @PostMapping
    public ResponseEntity<ReturnResponse> createReturn(
            @RequestHeader("X-Customer-Id") Long customerId,
            @Valid @RequestBody ReturnRequest request) {
        ReturnResponse response = returnService.createReturn(customerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my")
    public ResponseEntity<List<ReturnResponse>> getMyReturns(
            @RequestHeader("X-Customer-Id") Long customerId) {
        return ResponseEntity.ok(returnService.getReturnsByCustomer(customerId));
    }

    @GetMapping("/{returnId}")
    public ResponseEntity<ReturnResponse> getReturnById(
            @PathVariable Long returnId,
            @RequestHeader("X-Customer-Id") Long customerId) {
        return ResponseEntity.ok(returnService.getReturnById(returnId, customerId));
    }
}
