package com.entitykart.monolith.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.*;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${gateway.trusted-proxy:}")
    private String trustedProxy;

    private final Map<String, TokenBucket> loginRateLimiters   = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, TokenBucket> paymentRateLimiters = new java.util.concurrent.ConcurrentHashMap<>();

    private static class TokenBucket {
        private final double capacity;
        private final double refillRatePerSecond;
        private double tokens;
        private long lastRefillTime;
        volatile long lastAccessMillis;

        public TokenBucket(double capacity, double refillRatePerMinute) {
            this.capacity            = capacity;
            this.refillRatePerSecond = refillRatePerMinute / 60.0;
            this.tokens              = capacity;
            this.lastRefillTime      = System.currentTimeMillis();
            this.lastAccessMillis    = this.lastRefillTime;
        }

        public synchronized boolean tryConsume() {
            lastAccessMillis = System.currentTimeMillis();
            refill();
            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.currentTimeMillis();
            double elapsedSeconds = (now - lastRefillTime) / 1000.0;
            lastRefillTime = now;
            tokens = Math.min(capacity, tokens + elapsedSeconds * refillRatePerSecond);
        }
    }

    @Scheduled(fixedDelay = 600_000)
    public void evictStaleBuckets() {
        long cutoff = System.currentTimeMillis() - 10 * 60 * 1000L;
        int loginRemoved   = 0;
        int paymentRemoved = 0;
        for (Iterator<Map.Entry<String, TokenBucket>> it = loginRateLimiters.entrySet().iterator(); it.hasNext(); ) {
            if (it.next().getValue().lastAccessMillis < cutoff) { it.remove(); loginRemoved++; }
        }
        for (Iterator<Map.Entry<String, TokenBucket>> it = paymentRateLimiters.entrySet().iterator(); it.hasNext(); ) {
            if (it.next().getValue().lastAccessMillis < cutoff) { it.remove(); paymentRemoved++; }
        }
        if (loginRemoved + paymentRemoved > 0) {
            log.debug("Rate-limiter eviction: removed {} login + {} payment buckets", loginRemoved, paymentRemoved);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (trustedProxy != null && !trustedProxy.isBlank() && trustedProxy.equals(remoteAddr)) {
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isEmpty() && !"unknown".equalsIgnoreCase(xff)) {
                return xff.split(",")[0].trim();
            }
        }
        return remoteAddr;
    }

    private static final List<String> PUBLIC_ENDPOINTS = List.of(
            "/api/users/login",
            "/api/users/register",
            "/api/users/forgot-password",
            "/api/users/reset-password",
            "/api/users/refresh-token",
            "/actuator"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path   = request.getRequestURI();
        String method = request.getMethod();

        if ("OPTIONS".equalsIgnoreCase(method)) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = getClientIp(request);
        if ("/api/users/login".equals(path) && "POST".equalsIgnoreCase(method)) {
            TokenBucket bucket = loginRateLimiters.computeIfAbsent(ip, k -> new TokenBucket(10.0, 10.0));
            if (!bucket.tryConsume()) {
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Too many login attempts. Please try again later.\"}");
                return;
            }
        } else if ("/api/payments".equals(path) && "POST".equalsIgnoreCase(method)) {
            TokenBucket bucket = paymentRateLimiters.computeIfAbsent(ip, k -> new TokenBucket(5.0, 5.0));
            if (!bucket.tryConsume()) {
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Too many payment requests. Please try again later.\"}");
                return;
            }
        }

        String requestId = request.getHeader("X-Request-Id");
        if (requestId == null || requestId.trim().isEmpty()) {
            requestId = UUID.randomUUID().toString();
        }
        response.setHeader("X-Request-Id", requestId);

        HeaderMutatingRequestWrapper wrappedRequest = new HeaderMutatingRequestWrapper(request);
        wrappedRequest.addHeader("X-Request-Id", requestId);

        if (path.startsWith("/actuator")) {
            filterChain.doFilter(wrappedRequest, response);
            return;
        }

        // ── Non-API requests: static assets + SPA frontend → bypass JWT ─────────
        // JWT authentication is ONLY required for /api/** and /graphql endpoints.
        // Everything else (CSS, JS, HTML partials, images, SPA routes) must be
        // publicly accessible so the browser can load the AngularJS application.
        // Without this, requests to /css/style.css and /js/app.js return 401 JSON
        // instead of the actual files, causing a blank frontend page.
        if (!path.startsWith("/api/") && !path.startsWith("/graphql")) {
            filterChain.doFilter(wrappedRequest, response);
            return;
        }

        boolean isPublic = PUBLIC_ENDPOINTS.stream().anyMatch(path::startsWith);
        if (path.startsWith("/api/reviews")    && method.equalsIgnoreCase("GET")) isPublic = true;
        if (path.startsWith("/api/products")   && method.equalsIgnoreCase("GET")) isPublic = true;
        if (path.startsWith("/api/categories") && method.equalsIgnoreCase("GET")) isPublic = true;

        String token = null;
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        boolean isAuthenticatedViaCookie = false;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        } else {
            jakarta.servlet.http.Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (jakarta.servlet.http.Cookie cookie : cookies) {
                    if ("ek_access_token".equals(cookie.getName())) {
                        token = cookie.getValue();
                        isAuthenticatedViaCookie = true;
                        break;
                    }
                }
            }
        }

        String existingCsrf = null;
        jakarta.servlet.http.Cookie[] cookiesList = request.getCookies();
        if (cookiesList != null) {
            for (jakarta.servlet.http.Cookie c : cookiesList) {
                if ("XSRF-TOKEN".equals(c.getName())) {
                    existingCsrf = c.getValue();
                    break;
                }
            }
        }
        boolean isNewXsrf = false;
        if (existingCsrf == null) {
            isNewXsrf = true;
            existingCsrf = UUID.randomUUID().toString();
            ResponseCookie xsrfCookie = ResponseCookie.from("XSRF-TOKEN", existingCsrf)
                    .path("/")
                    .secure(false)
                    .httpOnly(false)
                    .sameSite("Lax")
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, xsrfCookie.toString());
        }

        boolean isUnsafe = "POST".equalsIgnoreCase(method) 
                || "PUT".equalsIgnoreCase(method) 
                || "DELETE".equalsIgnoreCase(method) 
                || "PATCH".equalsIgnoreCase(method);

        if (isAuthenticatedViaCookie && isUnsafe && !isNewXsrf) {
            String csrfHeader = request.getHeader("X-XSRF-TOKEN");
            if (csrfHeader == null || !existingCsrf.equals(csrfHeader)) {
                writeError(request, response, HttpServletResponse.SC_FORBIDDEN, "Invalid or missing CSRF token");
                return;
            }
        }

        if (token != null) {
            try {
                Key key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(key)
                        .build()
                        .parseClaimsJws(token)
                        .getBody();

                Long userId = claims.get("userId", Long.class);
                if (userId == null) {
                    Object userIdClaim = claims.get("userId");
                    if (userIdClaim instanceof Number) {
                        userId = ((Number) userIdClaim).longValue();
                    } else if (userIdClaim instanceof String) {
                        userId = Long.parseLong((String) userIdClaim);
                    }
                }
                String email = claims.get("email", String.class);
                String role  = claims.get("role", String.class);

                if (userId != null) {
                    wrappedRequest.addHeader("X-Customer-Id", String.valueOf(userId));
                    wrappedRequest.addHeader("X-User-Email", email);
                    wrappedRequest.addHeader("X-User-Role", role);

                    // §4.1 Critical security path added for /api/payments/all
                    if ((path.contains("/api/admin/")
                            || path.equals("/api/users/all")
                            || path.equals("/api/users/stats")
                            || path.equals("/api/payments/all")
                            || path.endsWith("/toggle-status")
                            || (path.startsWith("/api/orders/")
                                && (path.endsWith("/status") || path.endsWith("/payment-status"))))
                            && !"ADMIN".equalsIgnoreCase(role)) {
                        writeError(request, response, HttpServletResponse.SC_FORBIDDEN, "Access Denied: Admin role required");
                        return;
                    }

                    filterChain.doFilter(wrappedRequest, response);
                    return;
                }

                if (!isPublic) {
                    writeError(request, response, HttpServletResponse.SC_UNAUTHORIZED,
                               "Token is missing required claims (userId)");
                    return;
                }

            } catch (Exception e) {
                if (!isPublic) {
                    writeError(request, response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
                    return;
                }
            }
        } else {
            if (!isPublic) {
                writeError(request, response, HttpServletResponse.SC_UNAUTHORIZED, "Missing authentication token");
                return;
            }
        }

        filterChain.doFilter(wrappedRequest, response);
    }

    private void writeError(HttpServletRequest request, HttpServletResponse response, int status, String message) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"" + message + "\"}");
    }

    private static class HeaderMutatingRequestWrapper extends HttpServletRequestWrapper {
        private final Map<String, String> customHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        public HeaderMutatingRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        public void addHeader(String name, String value) {
            customHeaders.put(name, value);
        }

        @Override
        public String getHeader(String name) {
            String value = customHeaders.get(name);
            if (value != null) {
                return value;
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            names.addAll(customHeaders.keySet());
            Enumeration<String> superNames = super.getHeaderNames();
            while (superNames.hasMoreElements()) {
                names.add(superNames.nextElement());
            }
            return Collections.enumeration(names);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            String value = customHeaders.get(name);
            if (value != null) {
                return Collections.enumeration(Collections.singletonList(value));
            }
            return super.getHeaders(name);
        }
    }
}
