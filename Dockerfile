# ========================================================
# StockBridge Backend Dockerfile (Multi-stage build)
# Targets deployment on Render (Java 21 LTS)
# ========================================================

# Stage 1: Build & Package
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

# Copy Maven wrapper and POM first for layer caching
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy project source and build production artifact
COPY src src
RUN ./mvnw clean package -DskipTests -B

# Stage 2: Lightweight Production Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Run as non-root user for production security
RUN addgroup -S stockbridge && adduser -S stockbridge -G stockbridge

# Copy compiled JAR from builder stage
COPY --from=builder --chown=stockbridge:stockbridge /workspace/target/*.jar app.jar

USER stockbridge:stockbridge

EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
