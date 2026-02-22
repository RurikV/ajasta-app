# Frontend Dockerfile (React) - Alpine based
# Build stage
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
COPY . .
# Build frontend - ApiService resolves base URL at runtime
RUN npm ci --no-audit --no-fund && npm run build

# Runtime stage (nginx)
FROM nginx:1.27-alpine
# Copy custom nginx config for SPA routing and API proxy
COPY nginx.conf /etc/nginx/conf.d/default.conf
# Copy build artifacts
COPY --from=build /app/build /usr/share/nginx/html
EXPOSE 80
HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
  CMD wget -qO- http://localhost/ >/dev/null 2>&1 || exit 1
