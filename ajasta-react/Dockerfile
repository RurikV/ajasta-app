# Frontend Dockerfile (React) - Alpine based
# Build stage
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
# Allow overriding API base URL for different environments (local, cloud)
ARG API_BASE_URL="/api"
ENV API_BASE_URL=${API_BASE_URL}
COPY . .
# Build frontend; ApiService resolves base at runtime, but allow override via API_BASE_URL
RUN npm ci --no-audit --no-fund && \
  sed -i "s#static BASE_URL = \"http://localhost:8090/api\";#static BASE_URL = \"${API_BASE_URL}\";#" src/services/ApiService.js || true && \
  npm run build

# Runtime stage (nginx)
FROM nginx:1.27-alpine
# Copy custom nginx config for SPA routing
COPY nginx.conf /etc/nginx/conf.d/default.conf
# Copy build artifacts
COPY --from=build /app/build /usr/share/nginx/html
EXPOSE 80
HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
  CMD wget -qO- http://localhost/ >/dev/null 2>&1 || exit 1
