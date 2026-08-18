package com.entitykart.monolith.controller;

import com.entitykart.monolith.entity.NotificationEntity;
import com.entitykart.monolith.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<NotificationEntity>> getAll() {
        return ResponseEntity.ok(notificationService.getAllNotifications());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<NotificationEntity>> getByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(notificationService.getByUser(userId));
    }

    @GetMapping("/failed")
    public ResponseEntity<List<NotificationEntity>> getFailed() {
        return ResponseEntity.ok(notificationService.getFailedNotifications());
    }

    @PostMapping("/{id}/retry")
    public ResponseEntity<Map<String, String>> retry(@PathVariable Long id) {
        notificationService.retryFailed(id);
        return ResponseEntity.ok(Map.of("message", "Retry attempted for notification: " + id));
    }

    @PostMapping("/welcome")
    public ResponseEntity<Map<String, String>> sendWelcome(
            @RequestParam Long userId,
            @RequestParam String email,
            @RequestParam String name) {
        notificationService.handleWelcome(userId, email, name);
        return ResponseEntity.ok(Map.of("message", "Welcome email triggered for " + email));
    }
}
