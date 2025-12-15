# --- STAGE 1: Build the JAR file ---
FROM maven:3.9.6-amazoncorretto-21 AS build

WORKDIR /app

# Copy the entire backend folder to the root /app directory
COPY backend/ .
# Declare SERVICE_NAME for use in this stage
ARG SERVICE_NAME

    # Package the application
RUN mvn clean install -DskipTests -f pom.xml

    # --- STAGE 2: Create the final production image ---
FROM amazoncorretto:21-alpine-jdk

    # Re-declare ARG for this stage
ARG SERVICE_NAME

ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8443
EXPOSE 8080

WORKDIR /app

# Copy the JAR file from the build stage using the service name
COPY --from=build /app/${SERVICE_NAME}/target/*.jar app.jar

    ENTRYPOINT ["java", "-jar", "app.jar"]