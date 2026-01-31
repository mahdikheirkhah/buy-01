FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom + sources

COPY pom.xml .
COPY src ./src

# Build application
RUN mvn -q -DskipTests clean package

# ---- Run stage ----
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app

# Copy jar from build stage
COPY --from=build /app/target/discovery-service-0.0.1-SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]