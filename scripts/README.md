# Ajasta App — Unified Deployment System

🚀 **Streamlined deployment orchestrator with Docker Compose integration**

This repository contains a complete microservice application with both cloud VM and local development deployment options.

## Application Architecture

**Services:**
- **ajasta-backend** (Spring Boot, Java 21) - REST API server
- **ajasta-react** (React + Nginx) - Frontend web application  
- **PostgreSQL** (16-alpine) - Database server

**Features:**
- Unified deployment system for VM and local environments
- Docker Compose integration for simplified local development
- Comprehensive monitoring and management tools
- Automated cloud infrastructure provisioning

---

## Quick Start

### 🎯 One Command Deployment

```bash
# Deploy to cloud VM (default)
./scripts/deploy-all.zsh

# Deploy locally with Docker Compose  
./scripts/deploy-all.zsh --mode local

# Deploy to both VM and locally
./scripts/deploy-all.zsh --mode both
```

### 📋 Prerequisites

**For VM Deployment:**
- [Yandex Cloud CLI](https://cloud.yandex.com/docs/cli/) configured
- Docker and Docker Compose
- SSH client

**For Local Development:**
- Docker and Docker Compose
- Git

---

## Deployment Options

### ☁️ Cloud VM Deployment

Deploy to Yandex Cloud with full infrastructure automation:

```bash
# Basic deployment
./scripts/deploy-all.zsh --mode vm

# Clean deployment with custom DockerHub user
./scripts/deploy-all.zsh --mode vm --clean --dockerhub-user myuser

# Skip image building (use existing images)
./scripts/deploy-all.zsh --mode vm --skip-build
```

**What it creates:**
- VM instance with Ubuntu 22.04
- Static IP address  
- VPC network and subnet
- Service account with required permissions
- Docker containers running your application

**Access your application:**
- Frontend: `http://YOUR_VM_IP` (port 80)
- Backend API: `http://YOUR_VM_IP:8090`
- Database: `YOUR_VM_IP:15432`

### 🏠 Local Development

Use Docker Compose for local development:

```bash
# Start local development environment
./scripts/deploy-all.zsh --mode local

# Start with clean state
./scripts/deploy-all.zsh --mode local --clean

# Start without rebuilding images
./scripts/deploy-all.zsh --mode local --skip-build
```

**Includes development tools:**
- **Adminer** - Database administration at `http://localhost:8080`
- **Mailhog** - Email testing at `http://localhost:8025`
- **Java debugging** - Debug port 5005
- **Hot reloading** - Volume mounts for development

**Access your application:**
- Frontend: `http://localhost:3000`
- Backend API: `http://localhost:8090` 
- Database: `localhost:15432`

---

## Docker Compose Usage

### Basic Commands

```bash
# Start all services
docker-compose up -d

# View logs  
docker-compose logs -f

# Stop all services
docker-compose down

# Rebuild and start
docker-compose up -d --build
```

### Environment Configuration

1. **Copy the environment template:**
   ```bash
   cp .env.example .env
   ```

2. **Edit `.env` with your settings:**
   ```bash
   # Docker Configuration
   DOCKERHUB_USER=vladimirryrik
   
   # Database Settings
   POSTGRES_DB=ajastadb
   POSTGRES_USER=admin  
   POSTGRES_PASSWORD=adminpw
   
   # Application Settings
   JWT_SECRET=your-secret-key
   
   # Yandex Cloud (for VM deployment)
   YC_CLOUD_ID=your-cloud-id
   YC_FOLDER_ID=your-folder-id
   ```

3. **Deploy:**
   ```bash
   ./scripts/deploy-all.zsh --mode local
   ```

### Development Override

The `docker-compose.override.yml` automatically provides:

- **Development database** with separate credentials
- **Debug logging** enabled
- **Java remote debugging** on port 5005  
- **Volume mounts** for development files
- **Additional services** (Adminer, Mailhog)

---

## Management Tools

### 🔍 Status Monitoring

Check the health of your deployments:

```bash
# Check all deployments
./scripts/status-all.zsh

# Check specific deployment
./scripts/status-all.zsh --mode vm
./scripts/status-all.zsh --mode local

# Detailed status with logs
./scripts/status-all.zsh --verbose --logs --health
```

### 📋 Log Management

Aggregate and monitor logs:

```bash
# View logs from all services
./scripts/logs-all.zsh

# Follow logs in real-time
./scripts/logs-all.zsh --mode local --follow

# Show backend logs from last hour
./scripts/logs-all.zsh --service backend --since 1h

# Save logs to files
./scripts/logs-all.zsh --save
```

### 🧹 Cleanup and Maintenance

Clean up resources when needed:

```bash
# Interactive cleanup of both deployments
./scripts/cleanup-all.zsh

# Force cleanup VM without prompts
./scripts/cleanup-all.zsh --mode vm --force

# Clean containers but keep data and images
./scripts/cleanup-all.zsh --keep-data --keep-images

# Deep clean everything
./scripts/cleanup-all.zsh --deep-clean
```

---

## Manual Docker Commands

If you prefer manual container management:

### Build Images

```bash
# Build with platform targeting for compatibility
docker build --platform linux/amd64 -t vladimirryrik/ajasta-backend:alpine ./ajasta-backend
docker build --platform linux/amd64 -t vladimirryrik/ajasta-frontend:alpine ./ajasta-react  
docker build --platform linux/amd64 -t vladimirryrik/ajasta-postgres:alpine ./ajasta-postgres
```

### Manual Container Setup

```bash
# Create network
docker network create ajasta-net

# Create volume  
docker volume create ajasta_pg_data

# Run PostgreSQL
docker run -d --name ajasta-postgres \
  --network ajasta-net \
  -p 15432:5432 \
  -e POSTGRES_DB=ajastadb \
  -e POSTGRES_USER=admin \
  -e POSTGRES_PASSWORD=adminpw \
  -v ajasta_pg_data:/var/lib/postgresql/data \
  postgres:16-alpine

# Run backend
docker run -d --name ajasta-backend \
  --network ajasta-net \
  -p 8090:8090 \
  -e DB_URL=jdbc:postgresql://ajasta-postgres:5432/ajastadb \
  -e DB_USERNAME=admin \
  -e DB_PASSWORD=adminpw \
  -e JWT_SECRET=change-me-production \
  vladimirryrik/ajasta-backend:alpine

# Run frontend  
docker run -d --name ajasta-frontend \
  --network ajasta-net \
  -p 3000:80 \
  vladimirryrik/ajasta-frontend:alpine
```

---

## Environment Variables

### Core Configuration

| Variable | Default | Description |
|----------|---------|-------------|
| `DOCKERHUB_USER` | `vladimirryrik` | DockerHub username for images |
| `POSTGRES_DB` | `ajastadb` | Database name |
| `POSTGRES_USER` | `admin` | Database username |
| `POSTGRES_PASSWORD` | `adminpw` | Database password |
| `JWT_SECRET` | `change-me-production-secret-key` | JWT signing secret |

### Optional Integrations

| Variable | Description |
|----------|-------------|
| `MAIL_USERNAME` | SMTP email username |
| `MAIL_PASSWORD` | SMTP email password |
| `AWS_ACCESS_KEY_ID` | AWS access key for S3 |
| `AWS_SECRET_ACCESS_KEY` | AWS secret key |
| `AWS_S3_BUCKET` | S3 bucket name |
| `STRIPE_PUBLIC_KEY` | Stripe public key |
| `STRIPE_SECRET_KEY` | Stripe secret key |

### Yandex Cloud (VM Deployment)

| Variable | Description |
|----------|-------------|  
| `YC_CLOUD_ID` | Your Yandex Cloud ID |
| `YC_FOLDER_ID` | Your folder ID |
| `YC_ZONE` | Deployment zone (default: `ru-central1-b`) |
| `SSH_USERNAME` | SSH user for VM access (default: `ajasta`) |

See `.env.example` for complete configuration options.

---

## Kubernetes Management

### 🔧 Configure Local kubectl Access

If you have deployed the application to a Kubernetes cluster, you can configure your local `kubectl` to access the cluster:

```bash
# Fetch kubeconfig from K8s master and configure local access
./scripts/fetch-kubeconfig.zsh
```

**What this script does:**
1. Reads master node information from `ansible-k8s/inventory.ini`
2. Connects to the K8s master node via SSH
3. Fetches the kubeconfig file from the master
4. Updates the server URL to use the external IP address
5. Merges into `~/.kube/config` with context name `ajasta-cluster`
6. Sets proper permissions (600)
7. Tests the connection and displays cluster information

**Prerequisites:**
- K8s cluster deployed and running
- SSH access to master node (uses `~/.ssh/id_rsa_k8s` or `~/.ssh/id_rsa`)
- `kubectl` installed locally
- `ansible-k8s/inventory.ini` file with master node information

**After configuration:**
```bash
# View cluster info
kubectl cluster-info

# Get nodes
kubectl get nodes

# Get all resources in ajasta namespace
kubectl get all -n ajasta

# Switch between contexts
kubectl config use-context ajasta-cluster
```

**Environment Variables:**
- `KUBECONFIG_CONTEXT`: Custom name for kubectl context (default: `ajasta-cluster`)
- `SSH_KEY`: SSH private key path (default: auto-detect `~/.ssh/id_rsa_k8s` or `~/.ssh/id_rsa`)

**Example with custom context name:**
```bash
KUBECONFIG_CONTEXT=my-cluster ./scripts/fetch-kubeconfig.zsh
```

---

## Troubleshooting

### Common Issues

**VM SSH Connection Failed:**
```bash
# Check VM status
yc compute instance get --name ajasta-host

# Add SSH key manually  
SSH_USERNAME=ajasta SSH_PUBKEY_FILE=./scripts/ajasta_ed25519.pub ./scripts/add-ssh-key.zsh ajasta-host

# Test connection
./scripts/ssh-ajasta.zsh 'echo Connection successful'
```

**Docker Containers Not Starting:**
```bash
# Check container logs
./scripts/logs-all.zsh --service backend

# Check system resources
docker system df

# Clean up if needed
./scripts/cleanup-all.zsh --mode local --force
```

**Port Conflicts:**
```bash
# Check what's using ports
sudo netstat -tlnp | grep -E ':(3000|8090|15432)'

# Use different ports in .env
echo "FRONTEND_PORT=3001" >> .env  
echo "BACKEND_PORT=8091" >> .env
echo "POSTGRES_PORT=15433" >> .env
```

### Getting Help

```bash
# Show help for any script
./scripts/deploy-all.zsh --help
./scripts/status-all.zsh --help  
./scripts/logs-all.zsh --help
./scripts/cleanup-all.zsh --help

# Check application status
./scripts/status-all.zsh --verbose

# View recent logs
./scripts/logs-all.zsh --lines 100
```

### Support Resources

- **VM Management:** Use Yandex Cloud Console for resource monitoring
- **Local Development:** Check Docker Desktop for container management
- **Database Access:** Use Adminer at `http://localhost:8080` (local) or connect directly to port 15432
- **API Testing:** Backend API documentation available at `/swagger-ui` endpoint

---

## Development Workflow

### Typical Development Process

1. **Setup environment:**
   ```bash
   cp .env.example .env
   # Edit .env with your settings
   ```

2. **Start development environment:**
   ```bash
   ./scripts/deploy-all.zsh --mode local
   ```

3. **Monitor services:**
   ```bash
   ./scripts/status-all.zsh --mode local --verbose
   ```

4. **View logs during development:**
   ```bash
   ./scripts/logs-all.zsh --mode local --follow
   ```

5. **Deploy to cloud for testing:**
   ```bash
   ./scripts/deploy-all.zsh --mode vm
   ```

6. **Cleanup when done:**
   ```bash
   ./scripts/cleanup-all.zsh --keep-data
   ```

### Code Changes

- **Backend:** Rebuild with `docker-compose up -d --build ajasta-backend`
- **Frontend:** Rebuild with `docker-compose up -d --build ajasta-frontend`  
- **Database:** Schema changes persist in volumes
- **Configuration:** Update `.env` and restart: `docker-compose restart`

---

## Architecture Notes

- **Frontend** serves static files via Nginx with SPA routing support
- **Backend** uses Spring Boot with PostgreSQL integration
- **Database** uses persistent volumes for data preservation  
- **Networking** uses custom Docker networks for service communication
- **Security** includes health checks, resource limits, and secure defaults
