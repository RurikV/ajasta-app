# Ajasta App — Dockerized Microservices 

This repository contains a microservice application consisting of:
- ajasta-backend (Spring Boot, Java 21)
- ajasta-react (React built with Node, served by Nginx)
- PostgreSQL (alpine-based)

Contents:
- ajasta-backend/Dockerfile — backend image (Alpine, multi-stage)
- ajasta-react/Dockerfile — frontend image (Alpine, Node build -> Nginx)
- ajasta-react/nginx.conf — Nginx config for SPA routing
- ajasta-postgres/Dockerfile — PostgreSQL (alpine-based) with sensible defaults
- scripts/backup_volumes.sh — volume backup script

Ports:
- Backend: 8090
- Frontend: 3000 on host (Nginx listens on 80 inside the container)
- PostgreSQL: 15432 on host (5432 inside the container)

Backend environment variables (see ajasta-backend/src/main/resources/application.properties):
- DB_URL (default jdbc:postgresql://localhost:15432/admin; for containers use jdbc:postgresql://ajasta-postgres:5432/ajastadb)
- DB_USERNAME, DB_PASSWORD
- JWT_SECRET, MAIL_USERNAME, MAIL_PASSWORD, AWS_*, STRIPE_* (optional)

1) Build images
- Postgres:
  docker build -t ajasta-postgres:alpine ./ajasta-postgres

- Backend:
  docker build -t ajasta-backend:alpine ./ajasta-backend

- Frontend (override API base URL at build time if needed):
  docker build --build-arg API_BASE_URL="http://localhost:8090/api" -t ajasta-frontend:alpine ./ajasta-react

2) Create a Docker network and a named volume
- Create network for the stack:
  docker network create ajasta-net

- Create volume for PostgreSQL data:
  docker volume create ajasta_pg_data

3) Run containers (without docker-compose)
- Run PostgreSQL:
  docker run -d --name ajasta-postgres \
    --network ajasta-net \
    -p 15432:5432 \
    -e POSTGRES_DB=ajastadb \
    -e POSTGRES_USER=admin \
    -e POSTGRES_PASSWORD=adminpw \
    -v ajasta_pg_data:/var/lib/postgresql/data \
    ajasta-postgres:alpine

- Run backend (connects to postgres via container name):
  docker run -d --name ajasta-backend \
    --network ajasta-net \
    -p 8090:8090 \
    -e DB_URL=jdbc:postgresql://ajasta-postgres:5432/ajastadb \
    -e DB_USERNAME=admin \
    -e DB_PASSWORD=adminpw \
    -e JWT_SECRET=change-me \
    ajasta-backend:alpine

- Run frontend (static site via Nginx):
  docker run -d --name ajasta-frontend \
    --network ajasta-net \
    -p 3000:80 \
    ajasta-frontend:alpine

4) Quick checks
- Frontend: http://localhost:3000
- Backend:  http://localhost:8090 (simple GET /)
- PostgreSQL: psql -h localhost -p 15432 -U admin -d ajastadb

5) Push images to Docker Hub
 - docker login
   To sign in with credentials on the command line, use 'docker login -u <username>'

Replace <DOCKERHUB_USERNAME> with your Docker Hub username.

- Postgres:
  docker tag ajasta-postgres:alpine <DOCKERHUB_USERNAME>/ajasta-postgres:alpine
  docker push <DOCKERHUB_USERNAME>/ajasta-postgres:alpine

- Backend:
  docker tag ajasta-backend:alpine <DOCKERHUB_USERNAME>/ajasta-backend:alpine
  docker push <DOCKERHUB_USERNAME>/ajasta-backend:alpine

- Frontend (build-time API URL example for production):
  docker build --build-arg API_BASE_URL="http://<PUBLIC_BACKEND_HOST>:8090/api" -t <DOCKERHUB_USERNAME>/ajasta-frontend:alpine ./ajasta-react
  docker push <DOCKERHUB_USERNAME>/ajasta-frontend:alpine

6) Backup and restore volumes
Use the provided script to back up named volumes (default: ajasta_pg_data) to ./backups.

- Backup:
  bash scripts/backup_volumes.sh

- Environment variables:
  - VOLUMES: space-separated volumes to back up (default: "ajasta_pg_data")
  - BACKUP_DIR: destination directory (default: "./backups")

- Examples:
  VOLUMES="ajasta_pg_data" bash scripts/backup_volumes.sh
  BACKUP_DIR=/var/backups bash scripts/backup_volumes.sh

The script prints restore instructions at the end.

7) Stop and clean up
- Stop containers:
  docker stop ajasta-frontend ajasta-backend ajasta-postgres

- Remove containers:
  docker rm ajasta-frontend ajasta-backend ajasta-postgres

- Inspect network and volumes:
  docker network ls; docker network inspect ajasta-net
  docker volume ls; docker volume inspect ajasta_pg_data

Notes
- All Dockerfiles are located in their respective module directories.
- The frontend image bakes API_BASE_URL at build time using a sed replacement in src/services/ApiService.js.
- The backend listens on 8090 and reads configuration from environment variables.
