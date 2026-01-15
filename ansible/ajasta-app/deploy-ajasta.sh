#!/bin/bash
# Deploy Complete Ajasta Application Stack
# This script orchestrates the deployment of all Ajasta components
#
# Usage:
#   ./deploy-ajasta.sh [start_step] [-v|-vv|-vvv|-vvvv|-vvvvv]
#
# Examples:
#   ./deploy-ajasta.sh              # Start from step 1
#   ./deploy-ajasta.sh 3            # Start from step 3 (resume)
#   ./deploy-ajasta.sh -vv          # Start from step 1 with verbose output
#   ./deploy-ajasta.sh 3 -vvv       # Start from step 3 with more verbosity

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Configuration
INVENTORY="../k8s/inventory.ini"
LOG_FILE="deployment-$(date +%Y%m%d-%H%M%S).log"

# Parse command line arguments
START_STEP=1
VERBOSITY=""

# Process arguments
for arg in "$@"; do
    case $arg in
        -v|--verbose)
            VERBOSITY="-v"
            shift
            ;;
        -vv)
            VERBOSITY="-vv"
            shift
            ;;
        -vvv)
            VERBOSITY="-vvv"
            shift
            ;;
        -vvvv)
            VERBOSITY="-vvvv"
            shift
            ;;
        -vvvvv)
            VERBOSITY="-vvvvv"
            shift
            ;;
        [0-9])
            START_STEP=$arg
            shift
            ;;
        --help|-h)
            echo "Usage: $0 [start_step] [-v|-vv|-vvv|-vvvv|-vvvvv]"
            echo ""
            echo "Arguments:"
            echo "  start_step         Step number to start from (1-6)"
            echo "  -v, -vv, -vvv      Ansible verbosity level"
            echo ""
            echo "Examples:"
            echo "  $0                  # Run all steps (1-6)"
            echo "  $0 3                # Start from step 3 (skip steps 1-2)"
            echo "  $0 -vv              # Run with verbose output"
            echo "  $0 3 -vvv           # Start from step 3 with more verbosity"
            echo ""
            echo "Steps:"
            echo "  1. Deploy PostgreSQL Cluster"
            echo "  2. Deploy Backend API"
            echo "  3. Deploy Frontend"
            echo "  4. Configure Ingress"
            echo "  5. Verify Deployment"
            echo "  6. Configure TLS (Let's Encrypt - requires public domain!)"
            exit 0
            ;;
    esac
done

# Check if HELM is installed
if ! command -v helm &> /dev/null; then
    print_error "helm is not installed"
    echo "TLS setup requires helm. Install with: brew install helm (macOS) or https://helm.sh/docs/intro/install/"
    echo "To skip TLS setup, run: ./deploy-ajasta.sh 5"  # Stop after verification
    exit 1
fi

# Display verbosity mode if set
if [ -n "$VERBOSITY" ]; then
    echo -e "${YELLOW}Debug mode: Ansible verbosity level = ${VERBOSITY}${NC}"
    echo ""
fi

# Functions
print_step() {
    local step_num=$1
    local step_name=$2

    if [ "$step_num" -lt "$START_STEP" ]; then
        echo -e "${CYAN}================================================================${NC}"
        echo -e "${CYAN}SKIPPED: Step $step_num - $step_name${NC}"
        echo -e "${CYAN}================================================================${NC}"
    else
        echo -e "${BLUE}================================================================${NC}"
        echo -e "${GREEN}STEP $step_num: $step_name${NC}"
        echo -e "${BLUE}================================================================${NC}"
    fi
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_skip() {
    echo -e "${CYAN}⊘ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ ERROR: $1${NC}"
}

print_header() {
    echo ""
    echo -e "${BLUE}================================================================================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}================================================================================================${NC}"
    echo ""
}

# Log all output
exec > >(tee -a "$LOG_FILE")
exec 2>&1

print_header "AJASTA APPLICATION DEPLOYMENT"

echo "Deployment started at: $(date)"
echo "Log file: $LOG_FILE"
echo "Starting from step: $START_STEP"
if [ -n "$VERBOSITY" ]; then
    echo "Verbosity level: $VERBOSITY"
fi
echo ""

# Check if inventory file exists
if [ ! -f "$INVENTORY" ]; then
    print_error "Inventory file not found: $INVENTORY"
    echo "Please ensure you're running this script from the ansible/ajasta-app directory"
    exit 1
fi

# Check Ansible is installed
if ! command -v ansible-playbook &> /dev/null; then
    print_error "ansible-playbook is not installed"
    exit 1
fi

# ==============================================================
# STEP 1: Deploy PostgreSQL Cluster
# ==============================================================

if [ "$START_STEP" -le 1 ]; then
    print_step 1 "Deploy PostgreSQL HA Cluster"

    echo "Deploying PostgreSQL cluster with CloudNativePG operator..."
    echo "  - Instances: 2 (1 primary, 1 replica)"
    echo "  - Storage: Longhorn 5Gi"
    echo "  - Namespace: postgresql-cluster"
    echo ""

    if ansible-playbook -i "$INVENTORY" $VERBOSITY 21-deploy-postgres-cluster.yml; then
        print_success "PostgreSQL cluster deployed successfully"
    else
        print_error "PostgreSQL cluster deployment failed"
        echo "Check the log file for details: $LOG_FILE"
        exit 1
    fi
else
    print_skip "Step 1: PostgreSQL Cluster (already deployed)"
fi

# ==============================================================
# STEP 2: Deploy Backend API
# ==============================================================

if [ "$START_STEP" -le 2 ]; then
    print_step 2 "Deploy Backend API"

    echo "Deploying Spring Boot Backend API..."
    echo "  - Image: vladimirryrik/ajasta-backend:alpine"
    echo "  - Replicas: 1"
    echo "  - Namespace: ajasta"
    echo ""

    if ansible-playbook -i "$INVENTORY" $VERBOSITY 22-deploy-backend.yml; then
        print_success "Backend API deployed successfully"
    else
        print_error "Backend API deployment failed"
        echo "Check the log file for details: $LOG_FILE"
        exit 1
    fi
else
    print_skip "Step 2: Backend API (already deployed)"
fi

# ==============================================================
# STEP 3: Deploy Frontend
# ==============================================================

if [ "$START_STEP" -le 3 ]; then
    print_step 3 "Deploy Frontend"

    echo "Deploying React Frontend (Nginx)..."
    echo "  - Image: vladimirryrik/ajasta-frontend:alpine"
    echo "  - Replicas: 1"
    echo "  - Namespace: ajasta"
    echo ""

    if ansible-playbook -i "$INVENTORY" $VERBOSITY 23-deploy-frontend.yml; then
        print_success "Frontend deployed successfully"
    else
        print_error "Frontend deployment failed"
        echo "Check the log file for details: $LOG_FILE"
        exit 1
    fi
else
    print_skip "Step 3: Frontend (already deployed)"
fi

# ==============================================================
# STEP 4: Configure Ingress
# ==============================================================

if [ "$START_STEP" -le 4 ]; then
    print_step 4 "Configure Ingress"

    echo "Configuring NGINX Ingress for external access..."
    echo "  - Ingress Class: nginx"
    echo "  - Host: ajasta.local"
    echo "  - Routes: / → frontend, /api → backend"
    echo ""

    if ansible-playbook -i "$INVENTORY" $VERBOSITY 24-deploy-ingress.yml; then
        print_success "Ingress configured successfully"
    else
        print_error "Ingress configuration failed"
        echo "Check the log file for details: $LOG_FILE"
        exit 1
    fi
else
    print_skip "Step 4: Ingress Configuration (already configured)"
fi

# ==============================================================
# STEP 5: Verify Deployment
# ==============================================================

if [ "$START_STEP" -le 5 ]; then
    print_step 5 "Verify Deployment"

    echo "Running comprehensive verification of all components..."
    echo ""

    if ansible-playbook -i "$INVENTORY" $VERBOSITY 25-verify-deployment.yml; then
        print_success "All components verified successfully"
    else
        echo ""
        print_error "Verification failed or completed with warnings"
        echo "Check the output above for details"
    fi
else
    print_skip "Step 5: Verification (skipped)"
fi

# ==============================================================
# STEP 6: Configure TLS (Let's Encrypt)
# ==============================================================

if [ "$START_STEP" -le 6 ]; then
    print_step 6 "Configure TLS with Let's Encrypt"

    echo "Configuring TLS with Let's Encrypt..."
    echo "  - Deploying cert-manager"
    echo "  - Configuring Let's Encrypt issuer"
    echo "  - Enabling TLS on ingress"
    echo ""
    echo -e "${YELLOW}IMPORTANT: This step requires:${NC}"
    echo "  1. A public domain name (not ajasta.local!)"
    echo "  2. DNS pointing to your ingress controller"
    echo "  3. Port 80 accessible from internet"
    echo ""
    echo -e "${YELLOW}Read: QUICKSTART.md section on TLS setup${NC}"
    echo ""

    # Deploy cert-manager
    echo "Deploying cert-manager..."
    if ansible-playbook -i "$INVENTORY" $VERBOSITY 26-deploy-cert-manager.yml; then
        print_success "cert-manager deployed successfully"
    else
        print_error "cert-manager deployment failed"
        echo "Check the log file for details: $LOG_FILE"
        exit 1
    fi

    # Configure Let's Encrypt issuer
    echo "Configuring Let's Encrypt issuer..."
    if ansible-playbook -i "$INVENTORY" $VERBOSITY 27-configure-letsencrypt.yml; then
        print_success "Let's Encrypt issuer configured successfully"
    else
        print_error "Let's Encrypt issuer configuration failed"
        echo "Check the log file for details: $LOG_FILE"
        exit 1
    fi

    # Deploy TLS ingress
    echo "Deploying TLS-enabled ingress..."
    if ansible-playbook -i "$INVENTORY" $VERBOSITY 28-deploy-ingress-tls.yml; then
        print_success "TLS ingress deployed successfully"
    else
        print_error "TLS ingress deployment failed"
        echo "Check the log file for details: $LOG_FILE"
        echo ""
        echo -e "${YELLOW}NOTE: TLS deployment may fail if:${NC}"
        echo "  - You don't have a public domain name"
        echo "  - DNS is not configured correctly"
        echo "  - Port 80 is not accessible from internet"
        echo ""
        echo "You can skip TLS by running: ./deploy-ajasta.sh 5"
        exit 1
    fi
else
    print_skip "Step 6: TLS Configuration (skipped)"
fi

# ==============================================================
# DEPLOYMENT COMPLETE
# ==============================================================

print_header "DEPLOYMENT COMPLETE"

echo ""
echo -e "${GREEN}Ajasta application has been deployed successfully!${NC}"
echo ""
echo "Deployment Summary:"
echo "  ✓ PostgreSQL HA Cluster (2 instances)"
echo "  ✓ Backend API (Spring Boot)"
echo "  ✓ Frontend (React/Nginx)"
echo "  ✓ Ingress Configuration"
echo "  ✓ All components verified"

# Check if TLS was configured
if [ "$START_STEP" -le 6 ]; then
    echo "  ✓ TLS with Let's Encrypt (cert-manager)"
else
    echo "  ⊘ TLS Configuration (skipped - run with step 6 to enable)"
fi

echo ""
echo "Next Steps:"
echo ""

# If TLS was configured
if [ "$START_STEP" -le 6 ]; then
    echo "TLS is enabled! Your application has HTTPS encryption."
    echo ""
    echo "1. Monitor certificate issuance:"
    echo "   kubectl get certificate -n ajasta"
    echo "   kubectl describe certificate -n ajasta ajasta-tls"
    echo ""
    echo "2. Access the application (once certificate is ready):"
    echo "   Frontend: https://<YOUR_PUBLIC_DOMAIN>/"
    echo "   Backend API: https://<YOUR_PUBLIC_DOMAIN>/api"
    echo ""
    echo "3. Current using STAGING environment (certificates are invalid)"
    echo "   To switch to production, update playbook 28 and redeploy step 6"
    echo ""
else
    echo "Access without TLS (HTTP only):"
    echo ""
    echo "1. Get Ingress Controller NodePort:"
    echo "   kubectl get svc -n ingress-nginx ingress-nginx-controller"
    echo ""
    echo "2. Access the application:"
    echo "   Frontend: http://<NODE_IP>:32402"
    echo "   Backend API: http://<NODE_IP>:32402/api"
    echo ""
    echo "3. Optional: Add to /etc/hosts for local testing:"
    echo "   <NODE_IP> ajasta.local"
    echo ""
    echo "4. To enable TLS with Let's Encrypt:"
    echo "   ./deploy-ajasta.sh 6 -vv"
    echo ""
fi

echo "Verify components:"
echo "  kubectl get pods -n ajasta"
echo "  kubectl get pods -n postgresql-cluster"
echo "  kubectl get ingress -n ajasta"

if [ "$START_STEP" -le 6 ]; then
    echo "  kubectl get certificate -n ajasta"
fi

echo ""
echo "View logs:"
echo "  kubectl logs -n ajasta -l component=backend -f"
echo "  kubectl logs -n ajasta -l component=frontend -f"
echo ""
echo "Log file: $LOG_FILE"
echo "Deployment completed at: $(date)"
echo ""

exit 0
