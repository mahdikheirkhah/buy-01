# -------- BUILD STAGE --------
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy parent and all modules
COPY backend/pom.xml ../
COPY backend/common backend/common

# Build common module first
RUN mvn -q -DskipTests -f ../pom.xml clean install

# Copy and build service
COPY backend/orders-service/pom.xml .
COPY backend/orders-service/src ./src

RUN mvn -q -DskipTests clean package


# -------- RUNTIME STAGE --------
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app

COPY --from=build /app/target/orders-service-0.0.1-SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
