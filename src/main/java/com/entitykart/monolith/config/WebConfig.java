package com.entitykart.monolith.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.url:http://localhost:9001}")
    private String appUrl;

    @Value("${app.cors.allowed-origins:http://localhost:9001,http://localhost:3000}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .toArray(String[]::new);

        boolean hasWildcard = Arrays.asList(origins).contains("*");

        var mapping = registry.addMapping("/**")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("X-Request-Id")
                .allowCredentials(true);

        if (hasWildcard) {
            // Spring 6 rule: allowedOrigins("*") + allowCredentials(true) = ILLEGAL (400 Bad Request)
            // Fix: use allowedOriginPatterns("*") which supports credentials with wildcard
            mapping.allowedOriginPatterns("*");
        } else {
            mapping.allowedOrigins(origins);
        }
    }

    // ── SPA Forward Controller ────────────────────────────────────────────────
    // AngularJS uses hashbang routing (#!/login), so server-side /login route
    // should never be hit in normal usage. But if a user bookmarks or types
    // a deep URL directly, forward it to index.html so AngularJS can handle it.
    @Controller
    static class SpaForwardController {

        private static final List<String> API_PREFIXES = List.of("/api/", "/actuator", "/graphql");

        @GetMapping(value = {"/{path:[^\\.]*}", "/**/{path:[^\\.]*}"})
        public String forward(jakarta.servlet.http.HttpServletRequest request) {
            String uri = request.getRequestURI();
            boolean isApiPath = API_PREFIXES.stream().anyMatch(uri::startsWith);
            if (isApiPath) {
                return null; // Let Spring handle API paths normally
            }
            return "forward:/index.html";
        }
    }
}

