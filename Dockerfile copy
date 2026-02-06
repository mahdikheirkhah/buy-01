# Stage 1: Build Angular app
FROM node:20 AS build
WORKDIR /app

# Copy package files and install dependencies
COPY package*.json ./
RUN npm install --legacy-peer-deps

# Copy source code
COPY . .
RUN npm run build -- --configuration production

# Stage 2: Serve with Nginx
FROM nginx:alpine AS production
WORKDIR /usr/share/nginx/html

# Remove default nginx static assets
RUN rm -rf ./*

# Copy built Angular app from previous stage (browser folder contains the actual static files)
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html

# Copy custom nginx config (with proper structure)
COPY nginx.conf /etc/nginx/nginx.conf

COPY certs /etc/nginx/certs

# Expose the port nginx will listen on (4200 instead of 80)
EXPOSE 4200

# Start nginx (serves the pre-built static files)
CMD ["nginx", "-g", "daemon off;"]
