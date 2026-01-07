# -------- BUILD STAGE --------
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy all backend code
COPY backend/ .

# Build common module first (without aggregator)
WORKDIR /app/common
RUN mvn -q -DskipTests clean install

# Now build the service
WORKDIR /app/media-service
RUN mvn -q -DskipTests clean package


# -------- RUNTIME STAGE --------
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app

COPY --from=build /app/media-service/target/media-service-0.0.1-SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
