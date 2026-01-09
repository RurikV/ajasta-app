#!/bin/bash
set -e

echo "=== Updating Kubernetes Configuration ==="
echo ""

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

# Default paths
TERRAFORM_OUTPUTS="${PROJECT_ROOT}/terraform/outputs.json"
KUBECONFIG="${HOME}/.kube/config"
KUBECONFIG_BACKUP="${KUBECONFIG}.backup.$(date +%Y%m%d_%H%M%S)"
SSH_USER="ajasta"
SSH_KEY="${HOME}/.ssh/id_rsa_k8s"

# Parse arguments
FORCE=false
SKIP_SSH=false
PRINT_IP_ONLY=false

while [[ $# -gt 0 ]]; do
  case $1 in
    -f|--force)
      FORCE=true
      shift
      ;;
    --skip-ssh)
      SKIP_SSH=true
      shift
      ;;
    --print-ip-only)
      PRINT_IP_ONLY=true
      shift
      ;;
    -h|--help)
      echo "Usage: $0 [OPTIONS]"
      echo ""
      echo "Options:"
      echo "  -f, --force        Force update even if IP hasn't changed"
      echo "  --skip-ssh         Skip fetching kubeconfig from master (use existing)"
      echo "  --print-ip-only    Only print the master IP, don't update kubeconfig"
      echo "  -h, --help         Show this help message"
      echo ""
      echo "This script:"
      echo "  1. Reads master IP from Terraform outputs"
      echo "  2. Fetches admin kubeconfig from master node"
      echo "  3. Updates ~/.kube/config with new master IP"
      echo ""
      exit 0
      ;;
    *)
      echo "Unknown option: $1"
      echo "Use -h for help"
      exit 1
      ;;
  esac
done

# Step 1: Check if Terraform outputs exist
if [ ! -f "$TERRAFORM_OUTPUTS" ]; then
  echo "❌ Terraform outputs not found: $TERRAFORM_OUTPUTS"
  echo "   Run terraform apply first or generate outputs manually"
  exit 1
fi

# Step 2: Extract master IP from Terraform outputs
echo "📖 Reading Terraform outputs..."
MASTER_IP=$(jq -r '.master_public_ip // .master_public_ip.value' "$TERRAFORM_OUTPUTS" 2>/dev/null || echo "")

if [ -z "$MASTER_IP" ] || [ "$MASTER_IP" = "null" ]; then
  echo "❌ Failed to extract master IP from Terraform outputs"
  echo "   Check that terraform/outputs.json is valid"
  exit 1
fi

echo "✅ Master IP: $MASTER_IP"

if [ "$PRINT_IP_ONLY" = true ]; then
  echo "$MASTER_IP"
  exit 0
fi

# Step 3: Check if IP has changed
if [ -f "$KUBECONFIG" ]; then
  CURRENT_IP=$(grep -oP 'server: https://\K[0-9.]+(?=:6443)' "$KUBECONFIG" 2>/dev/null || echo "")

  if [ -n "$CURRENT_IP" ] && [ "$CURRENT_IP" = "$MASTER_IP" ] && [ "$FORCE" = false ]; then
    echo "✅ Kubeconfig already points to $MASTER_IP (no update needed)"
    exit 0
  fi

  if [ -n "$CURRENT_IP" ]; then
    echo "📝 IP changed: $CURRENT_IP → $MASTER_IP"
  fi
fi

# Step 4: Create backup of existing kubeconfig
if [ -f "$KUBECONFIG" ]; then
  echo "💾 Backing up existing kubeconfig to: $KUBECONFIG_BACKUP"
  cp "$KUBECONFIG" "$KUBECONFIG_BACKUP"
fi

# Step 5: Fetch kubeconfig from master or update existing
if [ "$SKIP_SSH" = false ]; then
  echo "📡 Fetching admin kubeconfig from master node..."

  if ! ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no -o ConnectTimeout=10 \
    "${SSH_USER}@${MASTER_IP}" "sudo cat /etc/kubernetes/admin.conf" > /tmp/kubeconfig-admin.new 2>/dev/null; then
    echo "❌ Failed to fetch kubeconfig from master"
    echo "   Ensure:"
    echo "   - Master is accessible via SSH"
    echo "   - Kubernetes is bootstrapped on master"
    echo "   - You have sudo access on master"
    exit 1
  fi

  # Update server IP in fetched kubeconfig
  sed -i.bak "s/server: https:\/\/[^:]*:6443/server: https:\/\/$MASTER_IP:6443/" /tmp/kubeconfig-admin.new
  rm -f /tmp/kubeconfig-admin.new.bak

  # Update server IP and handle TLS certificate issue
  sed -i.bak "s/server: https:\/\/[^:]*:6443/server: https:\/\/$MASTER_IP:6443/" /tmp/kubeconfig-admin.new
  rm -f /tmp/kubeconfig-admin.new.bak

  # Apply TLS fix for public IP access (certificate issued for internal IPs only)
  python3 << PYTHON_EOF
import yaml
import sys

try:
  with open('/tmp/kubeconfig-admin.new', 'r') as f:
    config = yaml.safe_load(f)

  # Update cluster configuration for public IP access
  for cluster in config.get('clusters', []):
    if 'cluster' in cluster:
      # Remove certificate authority (required with insecure flag)
      cluster['cluster'].pop('certificate-authority-data', None)
      # Add insecure skip TLS verify (certificate issued for internal IPs only)
      cluster['cluster']['insecure-skip-tls-verify'] = True

  with open('/tmp/kubeconfig-admin.new', 'w') as f:
    yaml.dump(config, f, default_flow_style=False)

  print("✅ Applied TLS fix for public IP access")
except Exception as e:
  print(f"⚠️  Warning: Could not apply TLS fix: {e}")
  sys.exit(0)  # Don't fail, continue with original file
PYTHON_EOF

  # Copy to ~/.kube/config
  mkdir -p "$(dirname "$KUBECONFIG")"
  cp /tmp/kubeconfig-admin.new "$KUBECONFIG"
  chmod 600 "$KUBECONFIG"
  rm -f /tmp/kubeconfig-admin.new

  echo "✅ Kubeconfig updated from master (with TLS fix)"
else
  echo "📝 Updating existing kubeconfig with new IP..."

  if [ ! -f "$KUBECONFIG" ]; then
    echo "❌ Kubeconfig not found and --skip-ssh was used"
    echo "   Either fetch from master or remove --skip-ssh flag"
    exit 1
  fi

  # Update server IP in existing kubeconfig
  sed -i.bak "s/server: https:\/\/[^:]*:6443/server: https:\/\/$MASTER_IP:6443/" "$KUBECONFIG"
  rm -f "${KUBECONFIG}.bak"

  echo "✅ Kubeconfig updated in place"
fi

# Step 6: Set KUBECONFIG environment variable hint
echo ""
echo "✅ Kubernetes configuration updated successfully!"
echo ""
echo "📋 Cluster details:"
echo "   Server: https://$MASTER_IP:6443"
echo "   Config: $KUBECONFIG"
echo ""
echo "🔧 To use kubectl with this cluster:"
echo "   export KUBECONFIG=$KUBECONFIG"
echo "   kubectl get nodes"
echo ""
echo "🚀 Or add to your ~/.bashrc or ~/.zshrc:"
echo "   export KUBECONFIG=$KUBECONFIG"
echo ""

# Step 7: Verify connectivity
echo "🧪 Testing cluster connectivity..."
if export KUBECONFIG="$KUBECONFIG" && kubectl get nodes &>/dev/null; then
  echo "✅ Successfully connected to cluster!"
  echo ""
  echo "📊 Cluster nodes:"
  export KUBECONFIG="$KUBECONFIG"
  kubectl get nodes
else
  echo "⚠️  Warning: Could not verify cluster connectivity"
  echo "   The cluster might still be bootstrapping"
  echo "   Try again after bootstrap completes"
fi
