package com.entitykart.monolith.controller;

import com.entitykart.monolith.dto.UserDTO;
import com.entitykart.monolith.dto.LoginRequest;
import com.entitykart.monolith.dto.LoginResponse;
import com.entitykart.monolith.dto.UserSessionDTO;
import com.entitykart.monolith.entity.RefreshTokenEntity;
import com.entitykart.monolith.service.UserService;
import com.entitykart.monolith.service.JwtService;
import com.entitykart.monolith.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;

    @PostMapping("/register")
    public UserDTO register(@Valid @RequestBody UserDTO userDTO) {
        return userService.register(userDTO);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest loginRequest,
                               HttpServletRequest httpRequest) {
        LoginResponse response = userService.login(loginRequest);

        if (Boolean.TRUE.equals(loginRequest.getRememberMe())) {
            String userAgent   = httpRequest.getHeader("User-Agent");
            String ipAddress   = httpRequest.getRemoteAddr();
            String fingerprint = httpRequest.getHeader("X-Device-Fingerprint");
            String rawRefresh  = refreshTokenService.createRefreshToken(
                    response.getUserId(), userAgent, ipAddress,
                    fingerprint != null ? fingerprint : "unknown", true);
            response.setRefreshToken(rawRefresh);
        }

        return response;
    }

    @PostMapping("/refresh-token")
    public LoginResponse refreshToken(@RequestBody Map<String, String> body) {
        String rawToken = body.get("refreshToken");
        if (rawToken == null || rawToken.isBlank()) {
            throw new RuntimeException("refreshToken is required");
        }

        RefreshTokenEntity tokenEntity = refreshTokenService.validateAndGet(rawToken);
        Long userId = tokenEntity.getUserId();

        var userDTO = userService.getUserById(userId);
        String newAccessToken = jwtService.generateToken(userId, userDTO.getEmail(), userDTO.getRole());
        long expiresIn = jwtService.getExpiration();

        LoginResponse response = new LoginResponse(
                newAccessToken, userId, userDTO.getName(), userDTO.getEmail(),
                userDTO.getRole(), userDTO.getProfilePicURL(), expiresIn);
        response.setRefreshToken(rawToken);
        return response;
    }

    @PostMapping("/forgot-password")
    public void forgotPassword(@RequestParam String email) {
        userService.forgotPassword(email);
    }

    @PostMapping("/reset-password")
    public void resetPassword(@RequestBody Map<String, String> body) {
        String token       = body.get("token");
        String newPassword = body.get("newPassword");
        userService.resetPassword(token, newPassword);
    }

    @GetMapping("/sessions")
    public List<UserSessionDTO> getActiveSessions(
            @RequestHeader("X-Customer-Id") Long userId) {
        return refreshTokenService.getActiveSessions(userId);
    }

    @DeleteMapping("/sessions/{sessionId}")
    public void revokeSession(
            @PathVariable Long sessionId,
            @RequestHeader("X-Customer-Id") Long userId) {
        refreshTokenService.revokeSessionById(sessionId, userId);
    }

    @DeleteMapping("/sessions")
    public void revokeAllSessions(@RequestHeader("X-Customer-Id") Long userId) {
        refreshTokenService.revokeAllSessionsForUser(userId);
        log.info("All sessions revoked for userId={}", userId);
    }

    @GetMapping("/all")
    public Page<UserDTO> getAllUsers(
            @RequestHeader(value = "X-User-Role", required = false) String loggedInUserRole,
            Pageable pageable) {
        if (!"ADMIN".equalsIgnoreCase(loggedInUserRole)) {
            throw new RuntimeException("Access denied: Admin role required");
        }
        return userService.getAllUsers(pageable);
    }

    @GetMapping("/{id}")
    public UserDTO getUserById(
            @PathVariable Long id,
            @RequestHeader(value = "X-Customer-Id", required = false) Long loggedInUserId,
            @RequestHeader(value = "X-User-Role",   required = false) String loggedInUserRole) {
        if (loggedInUserId != null && !id.equals(loggedInUserId) && !"ADMIN".equalsIgnoreCase(loggedInUserRole)) {
            throw new RuntimeException("Access denied: Unauthorized to view this profile");
        }
        return userService.getUserById(id);
    }

    @PutMapping("/{id}")
    public UserDTO updateUser(@PathVariable Long id,
                              @RequestHeader(value = "X-Customer-Id", required = false) Long loggedInUserId,
                              @RequestHeader(value = "X-User-Role",   required = false) String loggedInUserRole,
                              @Valid @RequestBody UserDTO userDTO) {
        if (loggedInUserId != null && !id.equals(loggedInUserId) && !"ADMIN".equalsIgnoreCase(loggedInUserRole)) {
            throw new RuntimeException("Unauthorized to update this profile");
        }
        return userService.updateUser(id, userDTO);
    }

    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable Long id,
                           @RequestHeader(value = "X-Customer-Id", required = false) Long loggedInUserId,
                           @RequestHeader(value = "X-User-Role",   required = false) String loggedInUserRole) {
        if (loggedInUserId != null && !id.equals(loggedInUserId) && !"ADMIN".equalsIgnoreCase(loggedInUserRole)) {
            throw new RuntimeException("Unauthorized to delete this account");
        }
        userService.deleteUser(id);
    }

    @PostMapping("/deactivate")
    public void deactivateAccount(@RequestHeader("X-Customer-Id") Long userId) {
        userService.deleteUser(userId);
    }

    @PatchMapping("/{id}/toggle-status")
    public UserDTO toggleUserStatus(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Role", required = false) String loggedInUserRole) {
        if (!"ADMIN".equalsIgnoreCase(loggedInUserRole)) {
            throw new RuntimeException("Access denied: Admin role required");
        }
        return userService.toggleUserStatus(id);
    }

    @GetMapping("/stats")
    public Map<String, Object> getUserStats(
            @RequestHeader(value = "X-User-Role", required = false) String loggedInUserRole) {
        if (!"ADMIN".equalsIgnoreCase(loggedInUserRole)) {
            throw new RuntimeException("Access denied: Admin role required");
        }
        return userService.getUserStats();
    }
}
