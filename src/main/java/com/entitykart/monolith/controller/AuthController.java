package com.entitykart.monolith.controller;

import com.entitykart.monolith.dto.LoginRequest;
import com.entitykart.monolith.dto.LoginResponse;
import com.entitykart.monolith.dto.UserSessionDTO;
import com.entitykart.monolith.entity.RefreshTokenEntity;
import com.entitykart.monolith.entity.UserEntity;
import com.entitykart.monolith.service.JwtService;
import com.entitykart.monolith.service.RefreshTokenService;
import com.entitykart.monolith.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest loginRequest,
            @RequestHeader(value = "User-Agent", required = false) String userAgent,
            @RequestHeader(value = "X-Forwarded-For", required = false) String xForwardedFor,
            HttpServletRequest request,
            HttpServletResponse response) {

        String ipAddress = xForwardedFor != null ? xForwardedFor.split(",")[0].trim() : request.getRemoteAddr();
        String deviceFingerprint = userAgent != null ? userAgent : "unknown";

        LoginResponse loginResponse = userService.login(loginRequest);

        boolean rememberMe = Boolean.TRUE.equals(loginRequest.getRememberMe());
        String refreshToken = refreshTokenService.createRefreshToken(
                loginResponse.getUserId(), userAgent, ipAddress, deviceFingerprint, rememberMe);

        ResponseCookie accessCookie = ResponseCookie.from("ek_access_token", loginResponse.getToken())
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(30 * 24 * 60 * 60)
                .sameSite("Lax")
                .build();

        long refreshMaxAge = rememberMe ? (30L * 24 * 60 * 60) : (7L * 24 * 60 * 60);
        ResponseCookie refreshCookie = ResponseCookie.from("ek_refresh_token", refreshToken)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(refreshMaxAge)
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        return ResponseEntity.ok(loginResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(
            @CookieValue(value = "ek_refresh_token", required = false) String refreshTokenFromCookie,
            @RequestBody(required = false) Map<String, String> body,
            HttpServletResponse response) {

        String refreshToken = refreshTokenFromCookie;
        if (refreshToken == null && body != null) {
            refreshToken = body.get("refreshToken");
        }

        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            return ResponseEntity.status(401).body("Missing refresh token");
        }

        try {
            RefreshTokenEntity session = refreshTokenService.validateAndGet(refreshToken);
            
            UserEntity user = userService.getUserEntityById(session.getUserId());
            String newAccessToken = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole());
            String newRefreshToken = UUID.randomUUID().toString();

            refreshTokenService.rotateToken(refreshToken, newRefreshToken);

            ResponseCookie accessCookie = ResponseCookie.from("ek_access_token", newAccessToken)
                    .httpOnly(true)
                    .secure(false)
                    .path("/")
                    .maxAge(30 * 24 * 60 * 60)
                    .sameSite("Lax")
                    .build();

            ResponseCookie refreshCookie = ResponseCookie.from("ek_refresh_token", newRefreshToken)
                    .httpOnly(true)
                    .secure(false)
                    .path("/")
                    .maxAge(30 * 24 * 60 * 60)
                    .sameSite("Lax")
                    .build();

            response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
            response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

            return ResponseEntity.ok(Map.of(
                    "accessToken", newAccessToken,
                    "refreshToken", newRefreshToken
            ));
        } catch (Exception e) {
            log.error("Failed to refresh token: {}", e.getMessage());
            return ResponseEntity.status(401).body("Invalid or expired session");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @CookieValue(value = "ek_refresh_token", required = false) String refreshTokenFromCookie,
            @RequestBody(required = false) Map<String, String> body,
            HttpServletResponse response) {

        String refreshToken = refreshTokenFromCookie;
        if (refreshToken == null && body != null) {
            refreshToken = body.get("refreshToken");
        }

        if (refreshToken != null) {
            refreshTokenService.revokeSession(refreshToken);
        }

        ResponseCookie accessCookie = ResponseCookie.from("ek_access_token", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("ek_refresh_token", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        return ResponseEntity.ok("Logged out successfully");
    }

    @PostMapping("/logout-all")
    public ResponseEntity<?> logoutAll(@RequestHeader("X-Customer-Id") Long userId, HttpServletResponse response) {
        refreshTokenService.revokeAllUserSessions(userId);

        ResponseCookie accessCookie = ResponseCookie.from("ek_access_token", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("Strict")
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("ek_refresh_token", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("Strict")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        return ResponseEntity.ok("Logged out from all devices");
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<UserSessionDTO>> getSessions(@RequestHeader("X-Customer-Id") Long userId) {
        return ResponseEntity.ok(refreshTokenService.getActiveSessions(userId));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<?> revokeSession(
            @PathVariable Long sessionId,
            @RequestHeader("X-Customer-Id") Long userId) {
        refreshTokenService.revokeSessionById(sessionId, userId);
        return ResponseEntity.ok("Session revoked");
    }
}
