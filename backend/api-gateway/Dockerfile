# -------- BUILD STAGE --------
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy parent pom + common library
COPY pom.xml .
COPY common ./common
COPY api-gateway ./api-gateway

# Build common library first, then api-gateway
RUN mvn -q -DskipTests clean install -pl common && cd api-gateway && mvn -q -DskipTests clean package


# -------- RUNTIME STAGE --------
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app

COPY --from=build /app/api-gateway/target/api-gateway-0.0.1-SNAPSHOT.jar app.jar

# Copy SSL keystore
COPY src/main/resources/certs/keystore.p12 src/main/resources/certs/keystore.p12

EXPOSE 8443

ENTRYPOINT ["java", "-jar", "app.jar"]