# -------- BUILD STAGE --------
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy all backend code
COPY backend/ .

# Build parent POM first (without installing modules)
RUN mvn -q -N install

# Build common module
RUN mvn -q -DskipTests -pl common clean install

# Now build the service
RUN mvn -q -DskipTests -pl dummy-data clean package


# -------- RUNTIME STAGE --------
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app

COPY --from=build /app/dummy-data/target/dummy-data-0.0.1-SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
