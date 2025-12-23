#!/bin/bash

# Script to fetch kubeconfig from Kubernetes master and update it for external access
# Usage: ./scripts/update-kubeconfig.sh <master_ip> [ssh_key_path]

set -e

# Default values
SSH_KEY_PATH="${2:-~/.ssh/id_rsa}"
KUBECONFIG_PATH="${KUBECONFIG:-$HOME/.kube/config}"
BACKUP_PATH="$KUBECONFIG_PATH.backup.$(date +%s)"

# Check if master IP is provided
if [ -z "$1" ]; then
    echo "Usage: $0 <master_ip> [ssh_key_path]"
    echo "Example: $0 51.250.100.200 ~/.ssh/id_rsa"
    exit 1
fi

MASTER_IP="$1"
SSH_USER="ajasta"

echo "🔧 Updating kubeconfig for Kubernetes cluster at $MASTER_IP"

# Create backup of existing kubeconfig
if [ -f "$KUBECONFIG_PATH" ]; then
    echo "📦 Backing up existing kubeconfig to $BACKUP_PATH"
    cp "$KUBECONFIG_PATH" "$BACKUP_PATH"
fi

# Create .kube directory if it doesn't exist
mkdir -p "$(dirname "$KUBECONFIG_PATH")"

# Add master IP to known hosts to avoid SSH prompts
echo "🔐 Adding master IP to known hosts..."
ssh-keyscan -H "$MASTER_IP" >> ~/.ssh/known_hosts 2>/dev/null || true

# Test SSH connection first
echo "🔍 Testing SSH connection to $MASTER_IP..."
if ! ssh -i "$SSH_KEY_PATH" -o ConnectTimeout=10 -o BatchMode=yes "$SSH_USER@$MASTER_IP" "echo 'SSH connection successful'" 2>/dev/null; then
    echo "❌ Failed to connect to $MASTER_IP via SSH"
    echo "Please check:"
    echo "  1. SSH key: $SSH_KEY_PATH"
    echo "  2. Master IP: $MASTER_IP"
    echo "  3. SSH access to $SSH_USER@$MASTER_IP"
    exit 1
fi

# Fetch admin.conf from master
echo "📥 Fetching kubeconfig from master..."
if ssh -i "$SSH_KEY_PATH" "$SSH_USER@$MASTER_IP" "sudo cat /etc/kubernetes/admin.conf" > "$KUBECONFIG_PATH.temp"; then
    echo "✅ Successfully fetched kubeconfig from master"
else
    echo "❌ Failed to fetch kubeconfig from master"
    echo "Make sure sudo works and /etc/kubernetes/admin.conf exists on the master"
    exit 1
fi

# Update server URL in kubeconfig to use external IP
echo "🔄 Updating kubeconfig server URL from internal to external IP..."
if sed -i.bak "s|https://127.0.0.1:6443|https://$MASTER_IP:6443|g" "$KUBECONFIG_PATH.temp"; then
    echo "✅ Updated server URL to https://$MASTER_IP:6443"
else
    echo "❌ Failed to update server URL in kubeconfig"
    exit 1
fi

# Test the kubeconfig
echo "🧪 Testing kubeconfig..."
export KUBECONFIG="$KUBECONFIG_PATH.temp"

# Wait a moment for the API server to be ready
echo "⏳ Waiting for Kubernetes API server to be ready..."
for i in {1..30}; do
    if kubectl cluster-info --request-timeout=5s >/dev/null 2>&1; then
        echo "✅ Kubernetes API server is ready"
        break
    else
        echo "⏳ Attempt $i/30: API server not ready yet..."
        sleep 5
    fi

    if [ $i -eq 30 ]; then
        echo "❌ Kubernetes API server not ready after 150 seconds"
        echo "Check if the cluster is fully initialized"
        exit 1
    fi
done

# Test basic kubectl commands
echo "🔍 Testing kubectl connectivity..."
kubectl cluster-info
echo ""
echo "📊 Cluster nodes:"
kubectl get nodes -o wide

# Move temporary kubeconfig to final location
mv "$KUBECONFIG_PATH.temp" "$KUBECONFIG_PATH"

# Set proper permissions
chmod 600 "$KUBECONFIG_PATH"

echo ""
echo "✅ Kubeconfig successfully updated!"
echo ""
echo "📍 Configuration details:"
echo "   Master IP: $MASTER_IP"
echo "   Kubeconfig: $KUBECONFIG_PATH"
echo "   SSH Key: $SSH_KEY_PATH"
echo ""
echo "🚀 Next steps:"
echo "   1. Test: export KUBECONFIG=$KUBECONFIG_PATH"
echo "   2. Verify: kubectl get nodes"
echo "   3. Deploy: kubectl apply -f k8s/"
echo ""
echo "📋 Quick commands:"
echo "   export KUBECONFIG=$KUBECONFIG_PATH"
echo "   kubectl get all -n ajasta"
echo "   kubectl get ingress -n ajasta"