# -------- BUILD STAGE --------
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

COPY backend/pom.xml ../
COPY backend/common backend/common
COPY backend/api-gateway/pom.xml .
COPY backend/api-gateway/src ./src

RUN mvn -q -DskipTests clean package


# -------- RUNTIME STAGE --------
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app

COPY --from=build /app/target/api-gateway-0.0.1-SNAPSHOT.jar app.jar

# Copy SSL keystore
COPY backend/api-gateway/src/main/resources/certs/keystore.p12 src/main/resources/certs/keystore.p12

ENTRYPOINT ["java", "-jar", "app.jar"]
