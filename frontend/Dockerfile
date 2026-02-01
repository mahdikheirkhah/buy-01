# Stage 1: Build Angular app
FROM node:20 AS build
WORKDIR /app

# Copy package files and install dependencies
COPY package*.json ./
RUN npm install --legacy-peer-deps

# Copy source code and build
COPY . .
RUN npm run build -- --configuration production

# Stage 2: Serve with Nginx
FROM nginx:alpine AS production
WORKDIR /usr/share/nginx/html

# Remove default nginx static assets and config
RUN rm -rf ./* /etc/nginx/conf.d/default.conf

# Copy built Angular app from previous stage
# Angular build outputs to dist/frontend by default
COPY --from=build /app/dist/frontend/* /usr/share/nginx/html/

# Copy custom nginx config (with proper structure)
COPY nginx.conf /etc/nginx/nginx.conf

# Copy SSL certificates
COPY certs /etc/nginx/certs

# Expose HTTPS port only
EXPOSE 4200

# Ensure proper permissions and start nginx
RUN chmod -R 755 /usr/share/nginx/html
CMD ["nginx", "-g", "daemon off;"]