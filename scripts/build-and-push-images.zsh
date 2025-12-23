#!/usr/bin/env zsh
# Build and push Docker images for Ajasta containerized application
# This script builds the backend and frontend Docker images and pushes them to DockerHub
# 
# Prerequisites:
# - Docker installed and running
# - Docker Hub account (vladimirryrik) logged in: docker login
# - Internet connection
#
# Usage:
#   ./build-and-push-images.zsh [--local-only]
#   DOCKERHUB_USER=myuser ./build-and-push-images.zsh
#   
# Options:
#   --local-only    Build images locally but don't push to DockerHub
#
# Environment Variables:
#   DOCKERHUB_USER  DockerHub username (default: vladimirryrik)

set -euo pipefail
SCRIPT_DIR=$(cd -- "${0:A:h}" && pwd)
PROJECT_ROOT=$(cd "$SCRIPT_DIR/.." && pwd)

require_cmd() { for c in "$@"; do command -v "$c" >/dev/null 2>&1 || { echo "Missing command: $c" >&2; exit 1; }; done; }
require_cmd docker

DOCKERHUB_USER=${DOCKERHUB_USER:-vladimirryrik}
LOCAL_ONLY=false

# Parse arguments
if [[ ${1:-} == "--local-only" ]]; then
    LOCAL_ONLY=true
    echo "🏠 Local build mode: images will not be pushed to DockerHub"
fi

log() {
    echo "$(date '+%Y-%m-%d %H:%M:%S') $*"
}

echo "🐳 Building Ajasta Docker Images..."
echo "📁 Project root: $PROJECT_ROOT"
echo "🏷️ Docker Hub user: $DOCKERHUB_USER"
echo ""

cd "$PROJECT_ROOT"

# Check if directories exist
if [[ ! -d "ajasta-backend" ]]; then
    echo "❌ Error: ajasta-backend directory not found" >&2
    exit 1
fi

if [[ ! -d "ajasta-react" ]]; then
    echo "❌ Error: ajasta-react directory not found" >&2
    exit 1
fi

if [[ ! -d "ajasta-postgres" ]]; then
    echo "❌ Error: ajasta-postgres directory not found" >&2
    exit 1
fi

# Build PostgreSQL image (wrapper) - force amd64 platform
echo "🗄️ Building PostgreSQL image for amd64 platform..."
log "Building $DOCKERHUB_USER/ajasta-postgres:alpine"
cd ajasta-postgres
docker build --platform linux/amd64 -t "$DOCKERHUB_USER/ajasta-postgres:alpine" .
cd ..

# Build backend image - force amd64 platform
echo ""
echo "☕ Building backend image for amd64 platform (this may take several minutes for Maven dependencies)..."
log "Building $DOCKERHUB_USER/ajasta-backend:alpine"
cd ajasta-backend
docker build --platform linux/amd64 -t "$DOCKERHUB_USER/ajasta-backend:alpine" .
cd ..

# Build frontend image with API URL for external access - force amd64 platform
echo ""
echo "⚛️ Building frontend image for amd64 platform and external access via port 80..."
log "Building $DOCKERHUB_USER/ajasta-frontend:alpine"
cd ajasta-react

# Build frontend with relative API URL for Kubernetes Ingress routing
# The API_BASE_URL uses relative path "/api" which works with Ingress
# For VM deployments with external IPs, the initContainer in K8s manifests handles URL rewriting
docker build --platform linux/amd64 --build-arg API_BASE_URL="/api" -t "$DOCKERHUB_USER/ajasta-frontend:alpine" .
cd ..

# List built images
echo ""
echo "📋 Built images:"
docker images | grep -E "($DOCKERHUB_USER/ajasta-|IMAGE ID)" | head -4

if [[ "$LOCAL_ONLY" == "true" ]]; then
    echo ""
    echo "✅ Local build complete! Images are ready for local use."
    echo ""
    echo "🔧 To deploy to VM with these images:"
    echo "   DOCKERHUB_USER=$DOCKERHUB_USER ./scripts/main-containerized.zsh"
    echo ""
    echo "📊 To check status after deployment:"
    echo "   ./scripts/ssh-ajasta.zsh '/opt/ajasta/status.sh'"
    exit 0
fi

# Check Docker Hub login
echo ""
echo "🔐 Checking Docker Hub authentication..."
if ! docker info | grep -q "Username:"; then
    echo "⚠️ You may need to login to Docker Hub first:"
    echo "   docker login"
    echo ""
    echo "Continue anyway? (y/N)"
    read -r response
    if [[ ! "$response" =~ ^[Yy]$ ]]; then
        echo "Aborted by user"
        exit 1
    fi
fi

# Push images to Docker Hub
echo ""
echo "🚀 Pushing images to Docker Hub..."

log "Pushing $DOCKERHUB_USER/ajasta-postgres:alpine"
docker push "$DOCKERHUB_USER/ajasta-postgres:alpine"

log "Pushing $DOCKERHUB_USER/ajasta-backend:alpine"
docker push "$DOCKERHUB_USER/ajasta-backend:alpine"

log "Pushing $DOCKERHUB_USER/ajasta-frontend:alpine"  
docker push "$DOCKERHUB_USER/ajasta-frontend:alpine"

echo ""
echo "✅ All images successfully built and pushed!"
echo ""
echo "📊 Published images:"
echo "   - $DOCKERHUB_USER/ajasta-postgres:alpine"
echo "   - $DOCKERHUB_USER/ajasta-backend:alpine"
echo "   - $DOCKERHUB_USER/ajasta-frontend:alpine"
echo ""
echo "🔧 Next steps:"
echo "   1. Deploy containerized VM:"
echo "      DOCKERHUB_USER=$DOCKERHUB_USER ./scripts/main-containerized.zsh"
echo "   2. Check status:"
echo "      ./scripts/ssh-ajasta.zsh '/opt/ajasta/status.sh'"
echo "   3. Access your application:"
echo "      Frontend: http://YOUR_VM_IP (port 80)"
echo "      Backend:  http://YOUR_VM_IP:8090"
echo ""
echo "💡 Your frontend will be accessible on port 80 via the external IP address!"