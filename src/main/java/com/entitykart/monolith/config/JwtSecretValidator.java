package com.entitykart.monolith.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * JwtSecretValidator — Startup security guard.
 *
 * <p>Verifies that JWT signing secrets have been replaced with real values
 * before the application serves traffic. If the app is running in a
 * production environment (detected via Render's injected RENDER env var)
 * and still uses a placeholder secret, startup is aborted immediately.
 *
 * <p>This prevents the silent security hole where tokens signed with a
 * publicly-known placeholder secret are accepted as valid.
 *
 * @author Md Sadique Amin
 */
@Component
public class JwtSecretValidator {

    private static final Logger log = LoggerFactory.getLogger(JwtSecretValidator.class);

    /** Known placeholder values committed to the public repo — treat as compromised. */
    private static final Set<String> PLACEHOLDER_SECRETS = Set.of(
            "change_me_in_production_min_32_chars",
            "change_refresh_secret_in_production",
            "your_jwt_secret",
            "secret",
            ""
    );

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.refresh-secret}")
    private String jwtRefreshSecret;

    /**
     * Runs immediately after bean initialization, before the app accepts requests.
     * In a production environment (Render injects the RENDER env var),
     * throws {@link IllegalStateException} if either secret is a placeholder.
     * In local dev, logs a warning only.
     */
    @PostConstruct
    public void validateSecrets() {
        boolean isProduction = isRunningOnRender();

        boolean accessSecretUnsafe  = isPlaceholder(jwtSecret);
        boolean refreshSecretUnsafe = isPlaceholder(jwtRefreshSecret);

        if (accessSecretUnsafe || refreshSecretUnsafe) {
            String message = buildWarningMessage(accessSecretUnsafe, refreshSecretUnsafe);

            if (isProduction) {
                log.error("╔══════════════════════════════════════════════════════════════╗");
                log.error("║  FATAL: Insecure JWT secret detected in production!          ║");
                log.error("║  Set JWT_SECRET and JWT_REFRESH_SECRET in Render dashboard.  ║");
                log.error("║  Generate with: openssl rand -base64 48                      ║");
                log.error("╚══════════════════════════════════════════════════════════════╝");
                throw new IllegalStateException(
                        "Application startup aborted: " + message +
                        " Set JWT_SECRET and JWT_REFRESH_SECRET as environment variables in Render. " +
                        "Generate values with: openssl rand -base64 48"
                );
            } else {
                log.warn("⚠️  [DEV MODE] {}", message);
                log.warn("⚠️  Replace with real secrets before deploying to production.");
            }
        } else {
            log.info("✅ JWT secrets validated — both secrets are non-placeholder values.");
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Detects whether the application is running on Render's platform.
     * Render automatically injects the RENDER environment variable.
     */
    private boolean isRunningOnRender() {
        String renderEnv = System.getenv("RENDER");
        return renderEnv != null && !renderEnv.isBlank();
    }

    /**
     * Returns true if the secret is null, blank, or matches a known placeholder.
     */
    private boolean isPlaceholder(String secret) {
        if (secret == null || secret.isBlank()) {
            return true;
        }
        // Prefix-match catches variants like "change_me_in_production_min_32_chars_extra"
        for (String placeholder : PLACEHOLDER_SECRETS) {
            if (!placeholder.isBlank() && secret.startsWith(placeholder)) {
                return true;
            }
        }
        return PLACEHOLDER_SECRETS.contains(secret.trim());
    }

    private String buildWarningMessage(boolean accessUnsafe, boolean refreshUnsafe) {
        if (accessUnsafe && refreshUnsafe) {
            return "Both JWT_SECRET and JWT_REFRESH_SECRET are using placeholder values.";
        } else if (accessUnsafe) {
            return "JWT_SECRET is using a placeholder value.";
        } else {
            return "JWT_REFRESH_SECRET is using a placeholder value.";
        }
    }
}
