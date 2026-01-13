# Multi-stage Dockerfile for Vue.js application

# Build stage
FROM docker.io/library/node:20-alpine AS build

WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies
RUN npm ci

# Copy source code
COPY . .

# Build the application
RUN npm run build

# Runtime stage
FROM docker.io/library/nginx:1.25-alpine

# Copy built application
COPY --from=build /app/dist /usr/share/nginx/html

# Create writable directories for OpenShift and set permissions
RUN mkdir -p /var/cache/nginx/client_temp \
    /var/cache/nginx/proxy_temp \
    /var/cache/nginx/fastcgi_temp \
    /var/cache/nginx/uwsgi_temp \
    /var/cache/nginx/scgi_temp \
    /var/run \
    /var/log/nginx && \
    chmod -R 777 /var/cache/nginx \
    /var/run \
    /var/log/nginx && \
    chmod -R 755 /usr/share/nginx/html && \
    chgrp -R 0 /var/cache/nginx \
    /var/run \
    /var/log/nginx \
    /usr/share/nginx/html && \
    chmod -R g=u /var/cache/nginx \
    /var/run \
    /var/log/nginx \
    /usr/share/nginx/html

# Create custom nginx config for SPA routing and OpenShift compatibility
RUN echo 'server { \
    listen 8080; \
    server_name _; \
    root /usr/share/nginx/html; \
    index index.html; \
    \
    # Temp directories for OpenShift \
    client_body_temp_path /var/cache/nginx/client_temp; \
    proxy_temp_path /var/cache/nginx/proxy_temp; \
    fastcgi_temp_path /var/cache/nginx/fastcgi_temp; \
    uwsgi_temp_path /var/cache/nginx/uwsgi_temp; \
    scgi_temp_path /var/cache/nginx/scgi_temp; \
    \
    location / { \
        try_files $uri $uri/ /index.html; \
    } \
    location /api { \
        proxy_pass http://backend:8090; \
        proxy_http_version 1.1; \
        proxy_set_header Upgrade $http_upgrade; \
        proxy_set_header Connection "upgrade"; \
        proxy_set_header Host $host; \
        proxy_set_header X-Real-IP $remote_addr; \
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for; \
        proxy_set_header X-Forwarded-Proto $scheme; \
    } \
}' > /etc/nginx/conf.d/default.conf && \
    echo 'pid /var/run/nginx.pid;' > /etc/nginx/nginx.conf.d/pid.conf

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/ || exit 1

# Expose port 8080 (non-privileged)
EXPOSE 8080

# Run nginx as non-root (OpenShift assigns random UID, group 0)
USER 1001

# Run nginx
CMD ["nginx", "-g", "daemon off;"]
