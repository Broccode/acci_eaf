# Multi-stage Dockerfile for ACCI EAF Control Plane Service
# Optimized for security, performance, and minimal attack surface

# ==============================================================================
# Stage 1: Build Environment
# ==============================================================================
FROM gradle:8.7-jdk21-alpine AS builder

# Set working directory
WORKDIR /app

# Copy dependency files first for better layer caching
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY gradle/ ./gradle/

# Copy all source code and workspace configuration
COPY . .

# Build the application with optimizations
RUN gradle clean build -x test --no-daemon --parallel \
    && gradle bootJar --no-daemon \
    && ls -la apps/acci-eaf-control-plane/build/libs/

# ==============================================================================
# Stage 2: Frontend Build (Hilla/Vaadin)
# ==============================================================================
FROM node:18-alpine AS frontend-builder

WORKDIR /app

# Copy package files for dependency caching
COPY apps/acci-eaf-control-plane/src/main/frontend/package*.json ./

# Install dependencies
RUN npm ci --only=production

# Copy frontend source
COPY apps/acci-eaf-control-plane/src/main/frontend/ ./

# Build frontend assets
RUN npm run build

# ==============================================================================
# Stage 3: Security Scanning
# ==============================================================================
FROM builder AS security-scanner

# Install security scanning tools
RUN apk add --no-cache curl

# Download and run OWASP Dependency Check (optional - can be done in CI)
# RUN curl -L https://github.com/jeremylong/DependencyCheck/releases/download/v8.4.0/dependency-check-8.4.0-release.zip -o dc.zip \
#     && unzip dc.zip \
#     && ./dependency-check/bin/dependency-check.sh --project "Control Plane" --scan ./apps/acci-eaf-control-plane/build/libs/

# ==============================================================================
# Stage 4: Runtime Base
# ==============================================================================
FROM eclipse-temurin:21-jre-alpine AS runtime-base

# Create non-root user for security
RUN addgroup -g 1001 -S eaf && \
    adduser -u 1001 -S eaf -G eaf

# Install runtime dependencies and security tools
RUN apk add --no-cache \
    curl \
    dumb-init \
    tzdata \
    && rm -rf /var/cache/apk/*

# Set timezone
ENV TZ=UTC

# Create application directories
RUN mkdir -p /app/logs /app/tmp /app/config \
    && chown -R eaf:eaf /app

# ==============================================================================
# Stage 5: Production Image
# ==============================================================================
FROM runtime-base AS production

# Switch to non-root user
USER eaf

# Set working directory
WORKDIR /app

# Copy built application from builder stage
COPY --from=builder --chown=eaf:eaf /app/apps/acci-eaf-control-plane/build/libs/*.jar app.jar

# Copy frontend assets from frontend builder
COPY --from=frontend-builder --chown=eaf:eaf /app/dist/ /app/static/

# Copy configuration files
COPY --from=builder --chown=eaf:eaf /app/apps/acci-eaf-control-plane/src/main/resources/application*.yml /app/config/

# Set JVM options for containerized environment
ENV JAVA_OPTS="-Xms512m -Xmx1g \
    -XX:+UseG1GC \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+ExitOnOutOfMemoryError \
    -XX:+UseStringDeduplication \
    -Djava.security.egd=file:/dev/./urandom \
    -Dspring.profiles.active=prod"

# Application configuration
ENV SERVER_PORT=8080
ENV SPRING_CONFIG_LOCATION=classpath:/application.yml,file:/app/config/

# Health check configuration
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:${SERVER_PORT}/actuator/health/liveness || exit 1

# Expose application port
EXPOSE 8080

# Use dumb-init to handle signals properly
ENTRYPOINT ["dumb-init", "--"]

# Start the application
CMD exec java $JAVA_OPTS -jar app.jar

# ==============================================================================
# Stage 6: Development Image (Optional)
# ==============================================================================
FROM runtime-base AS development

# Install development tools
RUN apk add --no-cache \
    git \
    vim \
    bash \
    htop

# Switch to non-root user
USER eaf

WORKDIR /app

# Copy application
COPY --from=builder --chown=eaf:eaf /app/apps/acci-eaf-control-plane/build/libs/*.jar app.jar

# Development JVM options (more debugging, less optimization)
ENV JAVA_OPTS="-Xms256m -Xmx512m \
    -XX:+UseG1GC \
    -XX:+UseContainerSupport \
    -Djava.security.egd=file:/dev/./urandom \
    -Dspring.profiles.active=local \
    -Dlogging.level.com.axians.eaf=DEBUG"

EXPOSE 8080 5005

# Enable remote debugging on port 5005
CMD exec java $JAVA_OPTS -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 -jar app.jar

# ==============================================================================
# Build Commands:
# ==============================================================================
# Build production image:
# docker build --target production -t eaf/control-plane:latest .
#
# Build development image:
# docker build --target development -t eaf/control-plane:dev .
#
# Run production:
# docker run -p 8080:8080 \
#   -e DB_USERNAME=eaf_user \
#   -e DB_PASSWORD=eaf_password \
#   -e EAF_IAM_SERVICE_URL=http://iam:8081 \
#   -e NATS_URL=nats://nats:4222 \
#   eaf/control-plane:latest
#
# Run development with debugging:
# docker run -p 8080:8080 -p 5005:5005 \
#   -e DB_USERNAME=eaf_dev \
#   -e DB_PASSWORD=dev_password \
#   eaf/control-plane:dev
# ==============================================================================

# Metadata
LABEL org.opencontainers.image.title="ACCI EAF Control Plane Service"
LABEL org.opencontainers.image.description="Administrative service for ACCI EAF platform with Hilla frontend"
LABEL org.opencontainers.image.vendor="Axians EAF Team"
LABEL org.opencontainers.image.version="1.0.0"
LABEL org.opencontainers.image.licenses="Proprietary"
LABEL org.opencontainers.image.source="https://gitlab.com/axians/acci-eaf"
