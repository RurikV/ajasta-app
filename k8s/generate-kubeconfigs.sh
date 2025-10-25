#!/bin/bash
#
# Script to generate kubeconfig files for RBAC service accounts
# Usage: ./generate-kubeconfigs.sh
#
# Prerequisites:
# - kubectl configured with cluster-admin access
# - RBAC resources applied (manifests/12-rbac-*.yml)
# - Kubernetes 1.24+ (uses manual token creation)

set -e

NAMESPACE="ajasta"
CLUSTER_NAME=$(kubectl config view --minify -o jsonpath='{.clusters[0].name}')
CLUSTER_SERVER=$(kubectl config view --minify -o jsonpath='{.clusters[0].cluster.server}')
OUTPUT_DIR="./kubeconfigs"

echo "==> Generating kubeconfig files for RBAC users"
echo "Cluster: $CLUSTER_NAME"
echo "Server: $CLUSTER_SERVER"
echo "Namespace: $NAMESPACE"
echo ""

# Create output directory
mkdir -p "$OUTPUT_DIR"

# Function to create a long-lived token for a service account
create_token() {
    local sa_name=$1
    local token_name="${sa_name}-token"
    
    # Create a Secret with token for the service account (K8s 1.24+)
    kubectl apply -f - <<EOF
apiVersion: v1
kind: Secret
metadata:
  name: $token_name
  namespace: $NAMESPACE
  annotations:
    kubernetes.io/service-account.name: $sa_name
type: kubernetes.io/service-account-token
EOF
    
    # Wait for token to be populated
    echo "Waiting for token to be created for $sa_name..."
    for i in {1..30}; do
        TOKEN=$(kubectl get secret "$token_name" -n "$NAMESPACE" -o jsonpath='{.data.token}' 2>/dev/null || echo "")
        if [ -n "$TOKEN" ]; then
            echo "$TOKEN"
            return 0
        fi
        sleep 1
    done
    
    echo "ERROR: Token creation timeout for $sa_name" >&2
    return 1
}

# Function to generate kubeconfig for a service account
generate_kubeconfig() {
    local sa_name=$1
    local role_type=$2
    local output_file="${OUTPUT_DIR}/kubeconfig-${role_type}.yaml"
    
    echo "==> Generating kubeconfig for $sa_name ($role_type)"
    
    # Get or create token
    local token_name="${sa_name}-token"
    local token_base64=$(create_token "$sa_name")
    local token=$(echo "$token_base64" | base64 -d)
    
    # Get CA certificate
    local ca_cert=$(kubectl get secret "$token_name" -n "$NAMESPACE" -o jsonpath='{.data.ca\.crt}')
    
    # Generate kubeconfig
    cat > "$output_file" <<EOF
apiVersion: v1
kind: Config
clusters:
- name: $CLUSTER_NAME
  cluster:
    certificate-authority-data: $ca_cert
    server: $CLUSTER_SERVER
contexts:
- name: ${sa_name}@${CLUSTER_NAME}
  context:
    cluster: $CLUSTER_NAME
    namespace: $NAMESPACE
    user: $sa_name
current-context: ${sa_name}@${CLUSTER_NAME}
users:
- name: $sa_name
  user:
    token: $token
EOF
    
    echo "✓ Created: $output_file"
    echo ""
}

# Generate kubeconfig files for each service account
generate_kubeconfig "reader-user" "read"
generate_kubeconfig "writer-user" "write"
generate_kubeconfig "admin-user" "admin"

echo "==> All kubeconfig files generated successfully!"
echo ""
echo "To use a kubeconfig file:"
echo "  export KUBECONFIG=$OUTPUT_DIR/kubeconfig-read.yaml"
echo "  kubectl get pods -n $NAMESPACE"
echo ""
echo "Or use --kubeconfig flag:"
echo "  kubectl --kubeconfig=$OUTPUT_DIR/kubeconfig-read.yaml get pods -n $NAMESPACE"
echo ""
echo "Files created:"
ls -lh "$OUTPUT_DIR"
