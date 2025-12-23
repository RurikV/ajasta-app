#!/usr/bin/env bash
#
# kubectl-fix-macos.sh
# Automated script to fix kubectl authentication issues on macOS
#
# Usage: bash kubectl-fix-macos.sh
# Optional: Set YC_CLUSTER_NAME environment variable to skip Yandex Cloud cluster prompt
#

set -euo pipefail

echo "🔧 kubectl Authentication Fix Script for macOS"
echo "================================================"
echo ""

# 1) Backup existing kubeconfig
echo "📦 Step 1: Backing up existing kubeconfig..."
mkdir -p ~/.kube
if [ -f ~/.kube/config ]; then
  BACKUP_FILE=~/.kube/config.bak.$(date +%s)
  cp ~/.kube/config "$BACKUP_FILE"
  echo "✅ Backed up existing kubeconfig to: $BACKUP_FILE"
else
  echo "ℹ️  No existing kubeconfig found at ~/.kube/config"
fi
echo ""

# 2) Ensure kubectl is installed
echo "🔍 Step 2: Checking kubectl installation..."
if ! command -v kubectl >/dev/null 2>&1; then
  echo "⚠️  kubectl not found!"
  if command -v brew >/dev/null 2>&1; then
    echo "📥 Installing kubectl via Homebrew..."
    brew install kubectl
    echo "✅ kubectl installed successfully"
  else
    echo "❌ Homebrew not found. Please install kubectl manually:"
    echo "   https://kubernetes.io/docs/tasks/tools/install-kubectl-macos/"
    exit 1
  fi
else
  echo "✅ kubectl is already installed ($(kubectl version --client --short 2>/dev/null || kubectl version --client 2>&1 | head -n1))"
fi
echo ""

# 3) Show current contexts
echo "📋 Step 3: Checking existing contexts..."
if kubectl config get-contexts >/dev/null 2>&1; then
  echo "Available contexts:"
  kubectl config get-contexts || true
  CURRENT=$(kubectl config current-context 2>/dev/null || echo "none")
  echo "Current context: ${CURRENT}"
else
  echo "ℹ️  No contexts configured yet"
fi
echo ""

# 4) Try Docker Desktop context
echo "🐳 Step 4: Checking Docker Desktop Kubernetes..."
if kubectl config get-contexts 2>/dev/null | awk '{print $2}' | grep -qx "docker-desktop" 2>/dev/null; then
  echo "Found docker-desktop context, testing connection..."
  kubectl config use-context docker-desktop 2>/dev/null || true
  if kubectl cluster-info >/dev/null 2>&1; then
    echo "✅ Successfully connected via Docker Desktop!"
    echo ""
    echo "Cluster info:"
    kubectl cluster-info
    echo ""
    echo "Nodes:"
    kubectl get nodes
    echo ""
    echo "🎉 kubectl authentication fixed! You can now use kubectl commands."
    exit 0
  else
    echo "⚠️  docker-desktop context exists but is not reachable"
    echo "   Make sure Kubernetes is enabled in Docker Desktop settings"
  fi
else
  echo "ℹ️  Docker Desktop context not found"
fi
echo ""

# 5) Try Minikube
echo "🎡 Step 5: Checking Minikube..."
if command -v minikube >/dev/null 2>&1; then
  echo "Minikube CLI found, checking status..."
  if minikube status >/dev/null 2>&1; then
    echo "Minikube is running, updating context..."
    minikube update-context || true
    kubectl config use-context minikube 2>/dev/null || true
    if kubectl cluster-info >/dev/null 2>&1; then
      echo "✅ Successfully connected via Minikube!"
      echo ""
      echo "Cluster info:"
      kubectl cluster-info
      echo ""
      echo "Nodes:"
      kubectl get nodes
      echo ""
      echo "🎉 kubectl authentication fixed! You can now use kubectl commands."
      exit 0
    else
      echo "⚠️  Minikube context exists but is not reachable"
    fi
  else
    echo "ℹ️  Minikube is installed but not running"
    echo "   Start it with: minikube start"
  fi
else
  echo "ℹ️  Minikube not found"
fi
echo ""

# 6) Try Yandex Cloud
echo "☁️  Step 6: Checking Yandex Cloud..."
if command -v yc >/dev/null 2>&1; then
  echo "Yandex Cloud CLI found!"
  
  # Check if already authenticated
  if yc config list >/dev/null 2>&1; then
    echo "✅ Yandex Cloud CLI is configured"
  else
    echo "⚠️  Yandex Cloud CLI not authenticated, running yc init..."
    yc init || true
  fi
  
  echo ""
  echo "Fetching available Kubernetes clusters..."
  CLUSTERS=$(yc managed-kubernetes cluster list --format json 2>/dev/null || echo "[]")
  
  if [ "$CLUSTERS" != "[]" ] && [ -n "$CLUSTERS" ]; then
    echo "Available clusters:"
    echo "$CLUSTERS" | jq -r '.[] | [.name, .id, .status] | @tsv' 2>/dev/null || echo "$CLUSTERS"
    echo ""
    
    # Check for environment variable
    CLUSTER_NAME="${YC_CLUSTER_NAME:-}"
    
    if [ -z "$CLUSTER_NAME" ]; then
      echo "Enter the cluster name to configure (or press Enter to skip):"
      read -r CLUSTER_NAME
    else
      echo "Using cluster from YC_CLUSTER_NAME: $CLUSTER_NAME"
    fi
    
    if [ -n "$CLUSTER_NAME" ]; then
      echo "Fetching credentials for cluster: $CLUSTER_NAME"
      # Use --external to connect from local machine
      # Use --force to overwrite existing context
      if yc managed-kubernetes cluster get-credentials "$CLUSTER_NAME" --external --force 2>/dev/null; then
        echo "✅ Credentials retrieved successfully"
        echo ""
        if kubectl cluster-info >/dev/null 2>&1; then
          echo "✅ Successfully connected to Yandex Cloud cluster: $CLUSTER_NAME"
          echo ""
          echo "Cluster info:"
          kubectl cluster-info
          echo ""
          echo "Nodes:"
          kubectl get nodes
          echo ""
          echo "🎉 kubectl authentication fixed! You can now use kubectl commands."
          exit 0
        else
          echo "⚠️  Credentials retrieved but cluster is not reachable"
          echo "   This might be due to network/firewall restrictions"
        fi
      else
        echo "❌ Failed to retrieve credentials for cluster: $CLUSTER_NAME"
      fi
    else
      echo "ℹ️  Skipped Yandex Cloud cluster configuration"
    fi
  else
    echo "ℹ️  No Kubernetes clusters found in Yandex Cloud"
  fi
else
  echo "ℹ️  Yandex Cloud CLI (yc) not found"
  echo "   Install it from: https://cloud.yandex.com/docs/cli/quickstart"
fi
echo ""

# 7) Manual instructions
echo "❌ Automated fix was unable to establish kubectl connection"
echo ""
echo "📚 Manual fix instructions by platform:"
echo ""
echo "🐳 Docker Desktop:"
echo "   1. Open Docker Desktop"
echo "   2. Go to Settings → Kubernetes"
echo "   3. Enable Kubernetes and click Apply"
echo "   4. Wait for it to start (green light)"
echo "   5. Run: kubectl config use-context docker-desktop"
echo ""
echo "🎡 Minikube:"
echo "   brew install minikube"
echo "   minikube start"
echo "   kubectl config use-context minikube"
echo ""
echo "☁️  Yandex Cloud:"
echo "   brew install yandex-cloud/tap/yc"
echo "   yc init"
echo "   yc managed-kubernetes cluster get-credentials <cluster-name> --external"
echo ""
echo "🌐 Google Cloud (GKE):"
echo "   brew install --cask google-cloud-sdk"
echo "   gcloud auth login"
echo "   gcloud container clusters get-credentials <cluster> --region <region> --project <project>"
echo ""
echo "🌐 Amazon Web Services (EKS):"
echo "   brew install awscli"
echo "   aws configure"
echo "   aws eks update-kubeconfig --name <cluster> --region <region>"
echo ""
echo "🌐 Microsoft Azure (AKS):"
echo "   brew install azure-cli"
echo "   az login"
echo "   az aks get-credentials --resource-group <rg> --name <cluster>"
echo ""
echo "💡 Alternative: Use GitLab CI/CD kubeconfig"
echo "   If you have access to the KUBECONFIG_CONTENT variable from GitLab:"
echo "   1. Copy the base64 value from GitLab Settings → CI/CD → Variables"
echo "   2. Run: echo 'PASTE_BASE64_HERE' | base64 -d > ~/.kube/config"
echo "   3. Run: chmod 600 ~/.kube/config"
echo "   4. Run: kubectl cluster-info"
echo ""

exit 1
