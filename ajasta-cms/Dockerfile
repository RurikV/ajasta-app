# Build stage
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -q -e -DskipTests dependency:go-offline && \
 mvn -q -e -DskipTests package

# Runtime stage
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/ajasta-cms-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8091
ENV SERVER_PORT=8091
ENTRYPOINT ["java","-jar","/app/app.jar"]
