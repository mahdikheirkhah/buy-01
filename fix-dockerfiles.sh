#!/bin/bash

# Services to update
SERVICES=("api-gateway" "discovery-service" "product-service" "user-service" "media-service" "dummy-data")

for SERVICE in "${SERVICES[@]}"; do
  if [ -f "$SERVICE/Dockerfile" ]; then
    cat > "$SERVICE/Dockerfile" << 'EOF'
# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace

# Copy parent pom
COPY pom.xml .

# Copy service pom
COPY SERVICE_NAME/pom.xml ./SERVICE_NAME/

# Copy common module if exists
COPY common ./common 2>/dev/null || true

# Download dependencies
RUN mvn -f SERVICE_NAME/pom.xml dependency:go-offline -B || true

# Copy service source
COPY SERVICE_NAME/src ./SERVICE_NAME/src

# Build
RUN mvn -f pom.xml clean install -DskipTests -N
RUN mvn -f SERVICE_NAME/pom.xml clean package -DskipTests

# ---- Run stage ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/SERVICE_NAME/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
EOF

    # Replace SERVICE_NAME placeholder
    sed -i.bak "s/SERVICE_NAME/$SERVICE/g" "$SERVICE/Dockerfile"
    rm "$SERVICE/Dockerfile.bak"

    echo "✓ Updated $SERVICE/Dockerfile"
  fi
done

echo "Done! Run: docker compose build"
