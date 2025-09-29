#!/usr/bin/env zsh
# Deploy containerized Ajasta application on Yandex Cloud VM
# This script creates a VM with Docker and runs the 3-container microservice stack:
# - ajasta-postgres (PostgreSQL)
# - ajasta-backend (Spring Boot API)
# - ajasta-frontend (React + Nginx) - running on port 80
#
# Environment Variables:
#   DOCKERHUB_USER - DockerHub username (default: vladimirryrik)

set -euo pipefail
SCRIPT_DIR=$(cd -- "${0:A:h}" && pwd)
cd "$SCRIPT_DIR"

require_cmd() { for c in "$@"; do command -v "$c" >/dev/null 2>&1 || { echo "Missing command: $c" >&2; exit 1; }; done; }
require_cmd zsh yc jq

# Set DockerHub user from environment or default to vladimirryrik
DOCKERHUB_USER=${DOCKERHUB_USER:-vladimirryrik}

echo "🐳 Deploying Ajasta Containerized Application..."
echo "🏷️  Using DockerHub user: $DOCKERHUB_USER"
echo ""

# Create external network + subnet (reuse existing to avoid quota issues)
YC_NETWORK_NAME=external-ajasta-network \
YC_SUBNET_NAME=ajasta-external-segment \
YC_SUBNET_RANGE=172.16.17.0/28 \
./create-network.zsh

# Create service account and keys
YC_SA_NAME=otus ./create-sa.zsh

# Reserve a static IP for ajasta
ajasta_IP=$(YC_ADDRESS_NAME=ajasta-static-ip ./create-static-ip.zsh)
echo "🌐 ajasta static IP: $ajasta_IP"

# Create ajasta VM with containerized setup
# Ensure we have a public key; generate if missing
KEY_PUB=${SSH_PUBKEY_FILE:-./ajasta_ed25519.pub}
if [ ! -f "$KEY_PUB" ]; then
  echo "🔑 SSH public key '$KEY_PUB' not found. Generating a new ed25519 keypair..."
  KEY_PUB=$(zsh ./generate-ssh-key.zsh ./ajasta_ed25519 ajasta@ajasta-host)
  echo "Generated keypair. Public key: $KEY_PUB"
fi

echo "🚀 Creating containerized VM with Docker and 3 microservices..."
YC_SUBNET_NAME=ajasta-external-segment \
METADATA_YAML=./metadata-containerized.yaml \
SSH_USERNAME=${SSH_USERNAME:-ajasta} \
SSH_PUBKEY_FILE="$KEY_PUB" \
DOCKERHUB_USER="$DOCKERHUB_USER" \
./create-vm-static-ip.zsh ajasta-host

echo ""
echo "✅ Deployment initiated! The VM is being created and Docker containers are starting."
echo ""
echo "📊 Access your application:"
echo "   Frontend (React):    http://$ajasta_IP (port 80)"
echo "   Backend API:         http://$ajasta_IP:8090"
echo "   PostgreSQL:          $ajasta_IP:15432"
echo ""
echo "🔧 Useful commands:"
echo "   SSH to VM:           ./ssh-ajasta.zsh"
echo "   Check containers:    ./ssh-ajasta.zsh '/opt/ajasta/status.sh'"
echo "   Restart containers:  ./ssh-ajasta.zsh 'sudo systemctl restart ajasta-containers'"
echo ""
echo "⏳ Note: Container initialization may take 5-10 minutes. Use the status command to monitor progress."
echo ""
echo "📝 DockerHub images expected:"
echo "   - $DOCKERHUB_USER/ajasta-postgres:alpine"  
echo "   - $DOCKERHUB_USER/ajasta-backend:alpine"
echo "   - $DOCKERHUB_USER/ajasta-frontend:alpine"
echo ""
echo "💡 If DockerHub images don't exist, build and push them first:"
echo "   DOCKERHUB_USER=$DOCKERHUB_USER ./build-and-push-images.zsh"