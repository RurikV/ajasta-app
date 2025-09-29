# Backend Dockerfile (Spring Boot) - Alpine based
# Multi-stage build: builder (Maven + JDK) -> runtime (JRE)

# 1) Builder stage
FROM maven:3.9.8-eclipse-temurin-21-alpine AS builder
WORKDIR /app
# Cache dependencies first
COPY pom.xml .
# Copy sources and build
COPY src ./src
#COPY config ./config
RUN mvn -q -e -DskipTests dependency:go-offline && \
  mvn -q -DskipTests package

# 2) Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Install curl for potential health checks
RUN apk add --no-cache curl=8.14.1-r1
# Copy the fat jar from builder
COPY --from=builder /app/target/*.jar /app/app.jar
# Default port
EXPOSE 8090
# Environment variables (can be overridden at runtime)
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
# Simple healthcheck hitting actuator if available, fallback to root
# Adjust path if you expose actuator (e.g., /actuator/health)
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD sh -c 'curl -fsS http://localhost:8090/ || exit 1'
# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
