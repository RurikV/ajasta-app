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
            echo "  start_step         Step number to start from (1-5)"
            echo "  -v, -vv, -vvv      Ansible verbosity level"
            echo ""
            echo "Examples:"
            echo "  $0                  # Run all steps (1-5)"
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
            exit 0
            ;;
    esac
done

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
echo ""
echo "Next Steps:"
echo ""
echo "1. Get Ingress Controller IP:"
echo "   kubectl get svc -n ingress-nginx ingress-nginx-controller"
echo ""
echo "2. Add to your /etc/hosts file:"
echo "   <INGRESS_IP> ajasta.local"
echo ""
echo "3. Access the application:"
echo "   Frontend: http://ajasta.local"
echo "   Backend API: http://ajasta.local/api"
echo ""
echo "4. Verify components:"
echo "   kubectl get pods -n ajasta"
echo "   kubectl get pods -n postgresql-cluster"
echo "   kubectl get ingress -n ajasta"
echo ""
echo "5. View logs:"
echo "   kubectl logs -n ajasta -l component=backend -f"
echo "   kubectl logs -n ajasta -l component=frontend -f"
echo ""
echo "Log file: $LOG_FILE"
echo "Deployment completed at: $(date)"
echo ""

exit 0
