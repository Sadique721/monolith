# ─────────────────────────────────────────────────────────────────────────────
# EntityKart Monolith — Multi-stage Dockerfile
# Author : Md Sadique Amin <mdsadiqueamin721786@gmail.com>
# Runtime: Java 17 (eclipse-temurin:17-jre-alpine)
# Deploy : Render Web Service (Docker runtime)
# ─────────────────────────────────────────────────────────────────────────────

# ── Stage 1: Build ───────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS build

WORKDIR /app

# Install Gradle 9.3.0 directly — avoids relying on gradle-wrapper.jar in repo
RUN apk add --no-cache wget unzip && \
    wget -q https://services.gradle.org/distributions/gradle-9.3.0-bin.zip -O /tmp/gradle.zip && \
    unzip -q /tmp/gradle.zip -d /opt && \
    rm /tmp/gradle.zip
ENV PATH="${PATH}:/opt/gradle-9.3.0/bin"

# Copy build scripts (cache layer — only re-resolves deps if these change)
COPY build.gradle.kts settings.gradle.kts ./

# Pre-download dependencies (allows Docker layer caching on subsequent builds)
RUN gradle dependencies --no-daemon || true

# Copy source code
COPY src src

# ── Bundle frontend into Spring Boot static resources ─────────────────────────
# This makes Spring Boot serve the AngularJS frontend at "/" on the same origin
# as the API — no separate Render service, no CORS complexity needed.
# node_modules is excluded via .dockerignore
COPY frontend src/main/resources/static/

# Build the fat JAR
RUN gradle bootJar --no-daemon

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

# JVM Arguments optimized for Render Free Tier (512MB RAM, 0.1 CPU)
# -XX:TieredStopAtLevel=1 : Disables C2 compiler to drastically speed up startup and save CPU/Memory
# -Xss256k                : Reduces thread stack size to save off-heap memory
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:TieredStopAtLevel=1", \
  "-Xss256k", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
