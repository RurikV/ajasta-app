#!/usr/bin/env zsh
# Install Rancher dashboard on a Kubernetes cluster
# Usage:
#   install-rancher.zsh <MASTER_PUBLIC_IP>
# Environment:
#   SSH_USERNAME  - SSH user to connect as (default: ajasta)
#   SSH_KEY_FILE  - Path to private SSH key for the user (optional)
#   RANCHER_HOSTNAME - Hostname for Rancher (default: rancher.local)
#   RANCHER_VERSION - Rancher chart version (default: latest stable)
#
# Notes:
# - Installs cert-manager (required by Rancher)
# - Installs Rancher via Helm chart
# - Uses NodePort for access (no external LoadBalancer needed)

set -euo pipefail

MASTER_IP=${1:-}
SSH_USERNAME=${SSH_USERNAME:-ajasta}
SSH_KEY_FILE=${SSH_KEY_FILE:-}
RANCHER_HOSTNAME=${RANCHER_HOSTNAME:-rancher.local}
RANCHER_VERSION=${RANCHER_VERSION:-}

if [ -z "$MASTER_IP" ]; then
  echo "[install-rancher] MASTER_PUBLIC_IP is required as arg1" >&2
  exit 2
fi

ssh_common=( -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -o ConnectTimeout=10 )
if [ -n "$SSH_KEY_FILE" ]; then
  ssh_common+=( -i "$SSH_KEY_FILE" )
fi

function rsh() {
  local host=$1; shift
  ssh "${ssh_common[@]}" ${SSH_USERNAME}@${host} "$@"
}

echo "[install-rancher] Installing Rancher dashboard on cluster at ${MASTER_IP}..."

# Script to install Rancher on the master node
read -r -d '' INSTALL_RANCHER_SCRIPT <<'EOF' || true
set -euo pipefail

RANCHER_HOST=${RANCHER_HOST}

echo "[rancher] Installing Helm if not present..."
if ! command -v helm >/dev/null 2>&1; then
  curl -fsSL https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash
fi

echo "[rancher] Adding Helm repositories..."
helm repo add rancher-latest https://releases.rancher.com/server-charts/latest
helm repo add jetstack https://charts.jetstack.io
helm repo update

echo "[rancher] Installing cert-manager..."
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.13.3/cert-manager.crds.yaml

kubectl create namespace cert-manager 2>/dev/null || true
helm upgrade --install cert-manager jetstack/cert-manager \
  --namespace cert-manager \
  --version v1.13.3 \
  --wait

echo "[rancher] Waiting for cert-manager to be ready..."
kubectl wait --for=condition=Available --timeout=300s deployment/cert-manager -n cert-manager
kubectl wait --for=condition=Available --timeout=300s deployment/cert-manager-webhook -n cert-manager
kubectl wait --for=condition=Available --timeout=300s deployment/cert-manager-cainjector -n cert-manager

echo "[rancher] Installing Rancher..."
kubectl create namespace cattle-system 2>/dev/null || true

helm upgrade --install rancher rancher-latest/rancher \
  --namespace cattle-system \
  --set hostname=${RANCHER_HOST} \
  --set replicas=1 \
  --set bootstrapPassword=admin \
  --wait

echo "[rancher] Waiting for Rancher to be ready..."
kubectl wait --for=condition=Available --timeout=600s deployment/rancher -n cattle-system || true

echo "[rancher] Patching Rancher service to use NodePort..."
kubectl patch service rancher -n cattle-system -p '{"spec":{"type":"NodePort"}}'

# Get the NodePort
RANCHER_PORT=$(kubectl get service rancher -n cattle-system -o jsonpath='{.spec.ports[?(@.port==443)].nodePort}')
echo ""
echo "=========================================="
echo "Rancher Dashboard Installation Complete!"
echo "=========================================="
echo ""
echo "Access Rancher at: https://${RANCHER_HOST}:${RANCHER_PORT}"
echo "Or via IP: https://$(hostname -I | awk '{print $1}'):${RANCHER_PORT}"
echo ""
echo "Bootstrap Password: admin"
echo ""
echo "Note: You'll need to accept the self-signed certificate in your browser."
echo "On first login, you'll be prompted to set a new password."
echo ""
echo "NodePort: ${RANCHER_PORT}"
echo "=========================================="
EOF

# Execute the installation script on the master
ENCODED_SCRIPT=$(echo "$INSTALL_RANCHER_SCRIPT" | base64)
rsh "$MASTER_IP" "echo '$ENCODED_SCRIPT' | base64 -d | RANCHER_HOST='${RANCHER_HOSTNAME}' bash -l"

echo ""
echo "[install-rancher] Fetching access information..."
RANCHER_PORT=$(rsh "$MASTER_IP" "kubectl get service rancher -n cattle-system -o jsonpath='{.spec.ports[?(@.port==443)].nodePort}' 2>/dev/null || echo '443'")

echo ""
echo "=========================================="
echo "Rancher Installation Summary"
echo "=========================================="
echo "Master IP: ${MASTER_IP}"
echo "Rancher URL: https://${MASTER_IP}:${RANCHER_PORT}"
echo "Bootstrap Password: admin"
echo ""
echo "To access Rancher:"
echo "1. Open https://${MASTER_IP}:${RANCHER_PORT} in your browser"
echo "2. Accept the self-signed certificate warning"
echo "3. Login with password: admin"
echo "4. Set a new password when prompted"
echo "=========================================="
