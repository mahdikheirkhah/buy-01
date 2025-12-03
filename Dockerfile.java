# --- STAGE 1: Build the JAR file ---
FROM maven:3.9.6-amazoncorretto-21 AS build

WORKDIR /app

# ✅ FIX: Copy the CONTENTS of the backend folder to the root /app directory
# The backend folder contains the root pom.xml and all the service subfolders
COPY backend/ .

# ARG SERVICE_PATH will be passed from docker-compose (e.g., backend/auth-service)
# But inside the container, since we copied backend/* to root, the path is just auth-service
ARG SERVICE_PATH

# Package the application
# Since we copied backend/* to ., the root pom.xml is now at ./pom.xml
RUN mvn clean install -DskipTests -f pom.xml

# --- STAGE 2: Create the final production image ---
FROM amazoncorretto:21-alpine-jdk

ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8443
EXPOSE 8080

WORKDIR /app

# Copy the JAR file from the build stage
# We need to adjust the path logic here too.
# If SERVICE_PATH passed from docker-compose is "backend/api-gateway",
# we need to extract just the service name "api-gateway" because that's where it is in the container.
# Alternatively, we can just rely on a new ARG for the simple name.
ARG SERVICE_PATH

# Let's use a shell command to find the jar, as the SERVICE_PATH passed from outside might be complex
# "find" is safer than trying to do string manipulation in COPY
# But COPY doesn't support shell expansion directly.

# 💡 BETTER FIX: Let's change how we pass the argument.
# Let's just pass the SERVICE_NAME (e.g., "api-gateway") instead of the full path.
ARG SERVICE_NAME
COPY --from=build /app/${SERVICE_NAME}/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]