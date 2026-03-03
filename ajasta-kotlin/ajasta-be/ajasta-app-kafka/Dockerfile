# Build stage
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Install Gradle
RUN apk add --no-cache gradle

# Copy build files
COPY build.gradle.kts .
COPY settings.gradle.kts ../

# Copy source code
COPY src ./src

# Build the application using Shadow JAR
RUN gradle shadowJar --no-daemon -x test

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user
RUN addgroup -S ajasta && adduser -S ajasta -G ajasta
USER ajasta

# Copy the Shadow JAR from build stage
COPY --from=build /app/build/libs/*-all.jar app.jar

# Environment variables with defaults
ENV KAFKA_BOOTSTRAP_SERVERS=localhost:9092
ENV KAFKA_GROUP_ID=ajasta-group
ENV KAFKA_INPUT_TOPIC=ajasta-request
ENV KAFKA_OUTPUT_TOPIC=ajasta-response

# Run the application
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
