# Build stage - Build from ajasta-be directory context
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Install Gradle
RUN apk add --no-cache gradle

# Copy root build files first
COPY settings.gradle.kts ./
COPY build.gradle.kts ./

# Copy all module build files
COPY ajasta-common/build.gradle.kts ./ajasta-common/
COPY ajasta-lib-cor/build.gradle.kts ./ajasta-lib-cor/
COPY ajasta-stubs/build.gradle.kts ./ajasta-stubs/
COPY ajasta-repo-common/build.gradle.kts ./ajasta-repo-common/
COPY ajasta-repo-inmemory/build.gradle.kts ./ajasta-repo-inmemory/
COPY ajasta-biz/build.gradle.kts ./ajasta-biz/
COPY ajasta-api-v1-jackson/build.gradle.kts ./ajasta-api-v1-jackson/
COPY ajasta-api-v1-mappers/build.gradle.kts ./ajasta-api-v1-mappers/
COPY ajasta-app-common/build.gradle.kts ./ajasta-app-common/
COPY ajasta-app-spring/build.gradle.kts ./ajasta-app-spring/

# Copy source code
COPY ajasta-common/src ./ajasta-common/src
COPY ajasta-lib-cor/src ./ajasta-lib-cor/src
COPY ajasta-stubs/src ./ajasta-stubs/src
COPY ajasta-repo-common/src ./ajasta-repo-common/src
COPY ajasta-repo-inmemory/src ./ajasta-repo-inmemory/src
COPY ajasta-biz/src ./ajasta-biz/src
COPY ajasta-api-v1-jackson/src ./ajasta-api-v1-jackson/src
COPY ajasta-api-v1-mappers/src ./ajasta-api-v1-mappers/src
COPY ajasta-app-common/src ./ajasta-app-common/src
COPY ajasta-app-spring/src ./ajasta-app-spring/src

# Build the application
RUN gradle :ajasta-app-spring:bootJar --no-daemon -x test

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user
RUN addgroup -S ajasta && adduser -S ajasta -G ajasta
USER ajasta

# Copy the JAR from build stage
COPY --from=build /app/ajasta-app-spring/build/libs/*.jar app.jar

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
