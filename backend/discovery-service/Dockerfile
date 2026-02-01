FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy parent pom + common library
COPY pom.xml .
COPY common ./common
COPY discovery-service ./discovery-service

# Build common library first, then discovery-service
RUN mvn -q -DskipTests clean install -pl common && cd discovery-service && mvn -q -DskipTests clean package

# ---- Run stage ----
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app

# Copy jar from build stage
COPY --from=build /app/discovery-service/target/discovery-service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8761

ENTRYPOINT ["java", "-jar", "app.jar"]