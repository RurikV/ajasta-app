# Build stage - Build from ajasta-kotlin directory context
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Install Gradle
RUN apk add --no-cache gradle

# Copy root build files
COPY settings.gradle.kts ./
COPY build.gradle.kts ./
COPY gradle.properties ./gradle.properties

# Copy version catalog
COPY gradle/libs.versions.toml ./gradle/

# Copy build-plugin
COPY build-plugin ./build-plugin

# Copy ajasta-be modules
COPY ajasta-be/settings.gradle.kts ./ajasta-be/
COPY ajasta-be/build.gradle.kts ./ajasta-be/

# Copy specs directory (referenced by ajasta-be/build.gradle.kts)
COPY specs ./specs

# Copy all module build files
COPY ajasta-be/ajasta-common/build.gradle.kts ./ajasta-be/ajasta-common/
COPY ajasta-be/ajasta-lib-cor/build.gradle.kts ./ajasta-be/ajasta-lib-cor/
COPY ajasta-be/ajasta-stubs/build.gradle.kts ./ajasta-be/ajasta-stubs/
COPY ajasta-be/ajasta-repo-common/build.gradle.kts ./ajasta-be/ajasta-repo-common/
COPY ajasta-be/ajasta-repo-inmemory/build.gradle.kts ./ajasta-be/ajasta-repo-inmemory/
COPY ajasta-be/ajasta-repo-tests/build.gradle.kts ./ajasta-be/ajasta-repo-tests/
COPY ajasta-be/ajasta-repo-pgjvm/build.gradle.kts ./ajasta-be/ajasta-repo-pgjvm/
COPY ajasta-be/ajasta-biz/build.gradle.kts ./ajasta-be/ajasta-biz/
COPY ajasta-be/ajasta-api-v1-jackson/build.gradle.kts ./ajasta-be/ajasta-api-v1-jackson/
COPY ajasta-be/ajasta-api-v1-mappers/build.gradle.kts ./ajasta-be/ajasta-api-v1-mappers/
COPY ajasta-be/ajasta-app-common/build.gradle.kts ./ajasta-be/ajasta-app-common/
COPY ajasta-be/ajasta-app-spring/build.gradle.kts ./ajasta-be/ajasta-app-spring/
COPY ajasta-be/ajasta-app-kafka/build.gradle.kts ./ajasta-be/ajasta-app-kafka/

# Copy source code
COPY ajasta-be/ajasta-common/src ./ajasta-be/ajasta-common/src
COPY ajasta-be/ajasta-lib-cor/src ./ajasta-be/ajasta-lib-cor/src
COPY ajasta-be/ajasta-stubs/src ./ajasta-be/ajasta-stubs/src
COPY ajasta-be/ajasta-repo-common/src ./ajasta-be/ajasta-repo-common/src
COPY ajasta-be/ajasta-repo-inmemory/src ./ajasta-be/ajasta-repo-inmemory/src
COPY ajasta-be/ajasta-repo-tests/src ./ajasta-be/ajasta-repo-tests/src
COPY ajasta-be/ajasta-repo-pgjvm/src ./ajasta-be/ajasta-repo-pgjvm/src
COPY ajasta-be/ajasta-biz/src ./ajasta-be/ajasta-biz/src
COPY ajasta-be/ajasta-api-v1-jackson/src ./ajasta-be/ajasta-api-v1-jackson/src
COPY ajasta-be/ajasta-api-v1-mappers/src ./ajasta-be/ajasta-api-v1-mappers/src
COPY ajasta-be/ajasta-app-common/src ./ajasta-be/ajasta-app-common/src
COPY ajasta-be/ajasta-app-spring/src ./ajasta-be/ajasta-app-spring/src
COPY ajasta-be/ajasta-app-kafka/src ./ajasta-be/ajasta-app-kafka/src

# Build the application
WORKDIR /app/ajasta-be
RUN gradle :ajasta-app-spring:bootJar --no-daemon -x test

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user
RUN addgroup -S ajasta && adduser -S ajasta -G ajasta
USER ajasta

# Copy the JAR from build stage
COPY --from=build /app/ajasta-be/ajasta-app-spring/build/libs/*.jar app.jar

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
