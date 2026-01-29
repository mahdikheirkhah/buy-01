# -------- BUILD STAGE --------
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom + sources
COPY ../pom.xml .
COPY pom.xml .
COPY src ./src

RUN mvn -q -DskipTests clean package


# -------- RUNTIME STAGE --------
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app

COPY --from=build  /app/target/dummy-data-0.0.1-SNAPSHOT.jar app.jar
COPY src/main/resources/certs/keystore.p12 src/main/resources/certs/keystore.p12
ENTRYPOINT ["java", "-jar", "app.jar"]