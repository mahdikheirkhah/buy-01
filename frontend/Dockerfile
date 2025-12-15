# Stage 1: Build Angular app
FROM node:20 AS build
WORKDIR /app

# Copy package files and install dependencies
COPY package*.json ./
RUN npm install

# Copy source code and build
COPY . .

# Expose port 4200
EXPOSE 4200

# Run Angular dev server with HTTPS
CMD ["npx", "ng", "serve", "--host", "0.0.0.0", "--port", "4200", "--ssl", "true", "--ssl-cert", "certs/frontend-dev.pem", "--ssl-key", "certs/frontend-dev.key"]
