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
    // Catch-all for SPA deep links (e.g. /login without the #!)
    // If a route isn't found (404), forward it to index.html so AngularJS handles it.
    @Controller
    static class SpaForwardController implements org.springframework.boot.web.servlet.error.ErrorController {

        @org.springframework.web.bind.annotation.RequestMapping("/error")
        public String handleError(jakarta.servlet.http.HttpServletRequest request) {
            Object status = request.getAttribute(jakarta.servlet.RequestDispatcher.ERROR_STATUS_CODE);
            if (status != null && Integer.valueOf(status.toString()) == 404) {
                return "forward:/index.html";
            }
            // For other errors, let Spring's default error view handle it, or also forward
            return "forward:/index.html";
        }
    }
}

