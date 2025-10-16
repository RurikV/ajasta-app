#!/usr/bin/env zsh
# Fetch kubeconfig from K8s master node and configure local kubectl access
# 
# This script:
# 1. Connects to the K8s master node via SSH
# 2. Copies the kubeconfig from the master node
# 3. Updates the server URL to use the external IP
# 4. Merges it into ~/.kube/config with a named context
# 5. Sets the context as current
#
# Prerequisites:
# - SSH access to K8s master node
# - ansible-k8s/inventory.ini file with master node information
# - SSH key configured (~/.ssh/id_rsa by default)
#
# Usage:
#   ./scripts/fetch-kubeconfig.zsh
#
# Environment Variables:
#   KUBECONFIG_CONTEXT: Name for the kubectl context (default: ajasta-cluster)
#   SSH_KEY: SSH private key path (default: ~/.ssh/id_rsa)

set -euo pipefail
SCRIPT_DIR=$(cd -- "${0:A:h}" && pwd)
PROJECT_ROOT=$(cd "$SCRIPT_DIR/.." && pwd)

# Configuration
INVENTORY_FILE="$PROJECT_ROOT/ansible-k8s/inventory.ini"
KUBECONFIG_CONTEXT=${KUBECONFIG_CONTEXT:-ajasta-cluster}
# Try default key first (matching inventory.ini), fall back to K8s-specific key
if [ -z "${SSH_KEY:-}" ]; then
    if [ -f "$HOME/.ssh/id_rsa" ]; then
        SSH_KEY="$HOME/.ssh/id_rsa"
    elif [ -f "$HOME/.ssh/id_rsa_k8s" ]; then
        SSH_KEY="$HOME/.ssh/id_rsa_k8s"
    else
        SSH_KEY="$HOME/.ssh/id_rsa"
    fi
fi
LOCAL_KUBECONFIG="$HOME/.kube/config"
TEMP_KUBECONFIG="/tmp/k8s-master-kubeconfig-$$.yaml"

log() {
    echo "$(date '+%Y-%m-%d %H:%M:%S') $*"
}

error() {
    echo "ERROR: $*" >&2
    exit 1
}

# Check prerequisites
if [ ! -f "$INVENTORY_FILE" ]; then
    error "Inventory file not found: $INVENTORY_FILE"
fi

if [ ! -f "$SSH_KEY" ]; then
    error "SSH key not found: $SSH_KEY"
fi

# Extract master node information from inventory
log "Reading master node information from inventory..."
MASTER_IP=$(grep -A1 '^\[k8s_master\]' "$INVENTORY_FILE" | grep 'ansible_host=' | sed 's/.*ansible_host=\([0-9.]*\).*/\1/')
MASTER_USER=$(grep -A1 '^\[k8s_master\]' "$INVENTORY_FILE" | grep 'ansible_user=' | sed 's/.*ansible_user=\([^ ]*\).*/\1/')

if [ -z "$MASTER_IP" ]; then
    error "Could not extract master IP from inventory file"
fi

if [ -z "$MASTER_USER" ]; then
    MASTER_USER="ajasta"
    log "Using default master user: $MASTER_USER"
fi

log "Master node: $MASTER_USER@$MASTER_IP"

# Test SSH connection
log "Testing SSH connection to master node..."
if ! ssh -i "$SSH_KEY" \
    -o BatchMode=yes \
    -o ConnectTimeout=10 \
    -o StrictHostKeyChecking=no \
    -o UserKnownHostsFile=/dev/null \
    "$MASTER_USER@$MASTER_IP" "echo 'SSH connection successful'" >/dev/null 2>&1; then
    error "Cannot connect to master node via SSH. Check your SSH key and network connection."
fi

# Fetch kubeconfig from master
log "Fetching kubeconfig from master node..."
if ssh -i "$SSH_KEY" \
    -o BatchMode=yes \
    -o StrictHostKeyChecking=no \
    -o UserKnownHostsFile=/dev/null \
    "$MASTER_USER@$MASTER_IP" "test -f ~/.kube/config" 2>/dev/null; then
    # Use user's kubeconfig
    scp -i "$SSH_KEY" \
        -o StrictHostKeyChecking=no \
        -o UserKnownHostsFile=/dev/null \
        "$MASTER_USER@$MASTER_IP:~/.kube/config" "$TEMP_KUBECONFIG" >/dev/null 2>&1
elif ssh -i "$SSH_KEY" \
    -o BatchMode=yes \
    -o StrictHostKeyChecking=no \
    -o UserKnownHostsFile=/dev/null \
    "$MASTER_USER@$MASTER_IP" "sudo test -f /etc/kubernetes/admin.conf" 2>/dev/null; then
    # Fall back to admin.conf (requires sudo)
    log "Using /etc/kubernetes/admin.conf (requires sudo)..."
    ssh -i "$SSH_KEY" \
        -o StrictHostKeyChecking=no \
        -o UserKnownHostsFile=/dev/null \
        "$MASTER_USER@$MASTER_IP" "sudo cat /etc/kubernetes/admin.conf" > "$TEMP_KUBECONFIG"
else
    error "Kubeconfig not found on master node"
fi

if [ ! -s "$TEMP_KUBECONFIG" ]; then
    error "Failed to fetch kubeconfig from master node"
fi

log "Kubeconfig fetched successfully"

# Detect the internal server URL
INTERNAL_SERVER=$(grep 'server:' "$TEMP_KUBECONFIG" | head -1 | sed 's/.*server: //')
log "Original server URL: $INTERNAL_SERVER"

# Update server URL to use external IP
log "Updating server URL to use external IP ($MASTER_IP)..."
sed -i.bak "s|server: https://.*:6443|server: https://$MASTER_IP:6443|g" "$TEMP_KUBECONFIG"
rm -f "$TEMP_KUBECONFIG.bak"

NEW_SERVER=$(grep 'server:' "$TEMP_KUBECONFIG" | head -1 | sed 's/.*server: //')
log "Updated server URL: $NEW_SERVER"

# Rename context and cluster
log "Renaming context to '$KUBECONFIG_CONTEXT'..."
sed -i.bak "s/kubernetes/$KUBECONFIG_CONTEXT/g" "$TEMP_KUBECONFIG"
rm -f "$TEMP_KUBECONFIG.bak"

# Backup existing kubeconfig
if [ -f "$LOCAL_KUBECONFIG" ]; then
    log "Backing up existing kubeconfig to $LOCAL_KUBECONFIG.backup-$(date +%Y%m%d-%H%M%S)"
    cp "$LOCAL_KUBECONFIG" "$LOCAL_KUBECONFIG.backup-$(date +%Y%m%d-%H%M%S)"
fi

# Create ~/.kube directory if it doesn't exist
mkdir -p "$HOME/.kube"

# Merge kubeconfig
log "Merging kubeconfig into $LOCAL_KUBECONFIG..."
if [ -f "$LOCAL_KUBECONFIG" ]; then
    # Merge with existing config
    KUBECONFIG="$LOCAL_KUBECONFIG:$TEMP_KUBECONFIG" kubectl config view --flatten > "$LOCAL_KUBECONFIG.tmp"
    mv "$LOCAL_KUBECONFIG.tmp" "$LOCAL_KUBECONFIG"
else
    # No existing config, just copy
    cp "$TEMP_KUBECONFIG" "$LOCAL_KUBECONFIG"
fi

# Set permissions
chmod 600 "$LOCAL_KUBECONFIG"

# Add insecure-skip-tls-verify flag using kubectl config command
# This ensures the flag is properly added after merge
log "Configuring cluster to skip TLS verification..."
kubectl config set-cluster "$KUBECONFIG_CONTEXT" \
    --server="https://$MASTER_IP:6443" \
    --insecure-skip-tls-verify=true \
    --embed-certs=false >/dev/null 2>&1 || true

# Extract the actual context name that contains our cluster name
ACTUAL_CONTEXT=$(kubectl config get-contexts -o name | grep "$KUBECONFIG_CONTEXT" | head -1)

if [ -z "$ACTUAL_CONTEXT" ]; then
    log "Warning: Could not find context containing '$KUBECONFIG_CONTEXT'"
    log "Available contexts:"
    kubectl config get-contexts -o name
else
    # Set current context
    log "Setting current context to '$ACTUAL_CONTEXT'..."
    kubectl config use-context "$ACTUAL_CONTEXT" >/dev/null
fi

# Clean up
rm -f "$TEMP_KUBECONFIG"

# Test connection
log "Testing kubectl connection..."
if kubectl cluster-info --request-timeout=5s >/dev/null 2>&1; then
    echo ""
    echo "✅ Successfully configured kubectl for cluster '$KUBECONFIG_CONTEXT'"
    echo ""
    echo "📊 Cluster information:"
    kubectl cluster-info
    echo ""
    echo "🔧 Available contexts:"
    kubectl config get-contexts
    echo ""
    echo "💡 You can now use kubectl to manage your cluster:"
    echo "   kubectl get nodes"
    echo "   kubectl get pods -A"
    echo "   kubectl get all -n ajasta"
    echo "   kubectl apply -f k8s/manifests/09-frontend-deployment.yml"
    echo ""
    echo "ℹ️  Note: TLS certificate verification is disabled (insecure-skip-tls-verify: true)"
    echo "   This is required because the K8s API certificate is valid only for internal IPs."
    echo "   For production, consider regenerating certificates with external IP in SAN list."
    echo ""
    echo "To switch contexts: kubectl config use-context $KUBECONFIG_CONTEXT"
else
    echo ""
    echo "⚠️  Warning: Kubeconfig was configured but connection test failed."
    echo "   This might be due to firewall rules blocking port 6443."
    echo "   The kubeconfig has been added to $LOCAL_KUBECONFIG with TLS verification disabled."
    echo ""
    echo "To test connection:"
    echo "   kubectl cluster-info"
    echo "   kubectl get nodes"
    echo ""
    echo "To apply manifests:"
    echo "   kubectl apply -f k8s/manifests/09-frontend-deployment.yml"
    echo ""
    echo "ℹ️  Note: If you see x509 certificate errors, the insecure-skip-tls-verify flag should handle them."
    echo "   If connection still fails, check firewall rules for port 6443 access."
fi
