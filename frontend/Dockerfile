# --- STAGE 1: Build the Angular App ---
FROM node:20-alpine AS build

WORKDIR /app

# Copy package files
COPY package*.json .
RUN npm install

# Copy source files
COPY . .

# Build the app
RUN npm run build -- --output-path=./dist/browser

# --- STAGE 2: Serve the App using Nginx ---
FROM nginx:stable-alpine

# ✅ FIX: Remove 'frontend/' prefix. The file is at the root of the build context.
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy built files
COPY --from=build /app/dist/browser /usr/share/nginx/html

EXPOSE 80
EXPOSE 443

CMD ["nginx", "-g", "daemon off;"]
