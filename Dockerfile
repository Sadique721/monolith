# ─────────────────────────────────────────────────────────────────────────────
# EntityKart Monolith — Multi-stage Dockerfile
# Author : Md Sadique Amin <mdsadiqueamin721786@gmail.com>
# Runtime: Java 17 (eclipse-temurin:17-jre-alpine)
# Deploy : Render Web Service (Docker runtime)
# ─────────────────────────────────────────────────────────────────────────────

# ── Stage 1: Build ───────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS build

WORKDIR /app

# Copy Gradle wrapper first (cache layer — only re-downloads if wrapper changes)
COPY gradlew .
COPY gradle gradle
RUN chmod +x gradlew

# Copy build scripts (cache layer — only re-resolves deps if these change)
COPY build.gradle.kts settings.gradle.kts ./

# Pre-download dependencies (allows Docker layer caching on subsequent builds)
RUN ./gradlew dependencies --no-daemon || true

# Copy source code
COPY src src

# ── Bundle frontend into Spring Boot static resources ─────────────────────────
# This makes Spring Boot serve the AngularJS frontend at "/" on the same origin
# as the API — no separate Render service, no CORS complexity needed.
# node_modules is excluded via .dockerignore
COPY frontend src/main/resources/static/

# Build the fat JAR
RUN ./gradlew bootJar --no-daemon

# ── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

# Create a non-root user for security (Render best practice)
RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app

# Copy only the built JAR from the build stage
COPY --from=build /app/build/libs/entitykart-monolith.jar app.jar

# Run as non-root
USER spring:spring

# Render injects PORT env var — app reads ${PORT:8080} from application.yml
EXPOSE 8080

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
