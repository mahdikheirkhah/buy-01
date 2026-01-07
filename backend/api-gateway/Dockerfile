# -------- BUILD STAGE --------
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy all backend code
COPY backend/ .

# Build common module first (without aggregator)
WORKDIR /app/common
RUN mvn -q -DskipTests clean install

# Now build the service
WORKDIR /app/api-gateway
RUN mvn -q -DskipTests clean package


# -------- RUNTIME STAGE --------
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app

COPY --from=build /app/api-gateway/target/api-gateway-0.0.1-SNAPSHOT.jar app.jar

# Copy SSL keystore
COPY backend/api-gateway/src/main/resources/certs/keystore.p12 src/main/resources/certs/keystore.p12

ENTRYPOINT ["java", "-jar", "app.jar"]
