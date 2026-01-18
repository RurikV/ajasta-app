#!/bin/bash
# Master Deployment Orchestrator for Ajasta Multi-Environment Deployments
# This script orchestrates the complete deployment process for staging or production
#
# Usage:
#   ./deploy-to-environment.sh <environment> [--step <N>] [--verbosity|-v|-vv|-vvv]
#
# Examples:
#   ./deploy-to-environment.sh staging
#   ./deploy-to-environment.sh production --step 3
#   ./deploy-to-environment.sh staging -vv
#
# Environment Deployment Steps:
#   1. Generate Ansible inventory from Yandex Cloud
#   2. Setup Kubernetes cluster (k8s-cluster-setup.sh)
#   3. Deploy CloudNativePG operator and PostgreSQL cluster
#   4. Deploy Ajasta application (deploy-ajasta.sh)
#   5. Fix connection timeout issues

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
NC='\033[0m' # No Color

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
ANSIBLE_K8S_DIR="${PROJECT_ROOT}/ansible/k8s"
ANSIBLE_APP_DIR="${PROJECT_ROOT}/ansible/ajasta-app"
ENVIRONMENTS_DIR="${PROJECT_ROOT}/environments"

# Default values
ENVIRONMENT=""
START_STEP=1
VERBOSITY=""
DRY_RUN=false
SKIP_CONFIRMATION=false

# Functions
print_header() {
    echo ""
    echo -e "${MAGENTA}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${MAGENTA}  $1${NC}"
    echo -e "${MAGENTA}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""
}

print_step() {
    local step_num=$1
    local step_name=$2

    if [ "$step_num" -lt "$START_STEP" ]; then
        echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
        echo -e "${CYAN}SKIPPED: Step $step_num - $step_name${NC}"
        echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    else
        echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
        echo -e "${GREEN}STEP $step_num: $step_name${NC}"
        echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    fi
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ ERROR: $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  WARNING: $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

show_usage() {
    cat << EOF
${MAGENTA}Ajasta Multi-Environment Deployment Orchestrator${NC}

${GREEN}Usage:${NC}
  $0 <environment> [options]

${GREEN}Arguments:${NC}
  environment        Target environment (staging or production)

${GREEN}Options:${NC}
  --step <N>         Start deployment from step N (1-5)
  -v, -vv, -vvv      Ansible verbosity level
  --dry-run          Show what would be done without executing
  --yes, -y          Skip confirmation prompts
  --help, -h         Show this help message

${GREEN}Environments:${NC}
  staging            Deploy to staging environment
  production         Deploy to production environment

${GREEN}Deployment Steps:${NC}
  1. Generate Ansible inventory from Yandex Cloud VMs
  2. Setup Kubernetes cluster (components, networking, storage)
  3. Deploy CloudNativePG operator and PostgreSQL cluster
  4. Deploy Ajasta application (backend, frontend, ingress)
  5. Fix connection timeout issues

${GREEN}Examples:${NC}
  $0 staging                                    # Deploy all steps to staging
  $0 production -vv                             # Deploy to production with verbose output
  $0 staging --step 3                           # Start from step 3 (skip 1-2)
  $0 production --step 4 -vvv                   # Start from step 4 with very verbose output

${GREEN}Environment Configuration:${NC}
  Staging:
    - Domain: staging.ajasta.top
    - VM Prefix: ajasta-staging
    - Resources: Minimal (1 worker)
    - TLS: Let's Encrypt Staging

  Production:
    - Domain: ajasta.top
    - VM Prefix: ajasta-prod
    - Resources: Full (3 workers, HA database)
    - TLS: Let's Encrypt Production

EOF
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        staging|production)
            ENVIRONMENT="$1"
            shift
            ;;
        --step)
            START_STEP="$2"
            shift 2
            ;;
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
        --dry-run)
            DRY_RUN=true
            shift
            ;;
        --yes|-y)
            SKIP_CONFIRMATION=true
            shift
            ;;
        --help|-h)
            show_usage
            exit 0
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            echo "Use --help for usage information"
            exit 1
            ;;
    esac
done

# Validate environment
if [ -z "$ENVIRONMENT" ]; then
    print_error "Environment not specified!"
    echo ""
    echo "Usage: $0 <environment> [options]"
    echo "Run --help for more information"
    exit 1
fi

if [[ ! "$ENVIRONMENT" =~ ^(staging|production)$ ]]; then
    print_error "Invalid environment: $ENVIRONMENT"
    echo "Valid environments: staging, production"
    exit 1
fi

# Load environment configuration
ENV_CONFIG_FILE="${ENVIRONMENTS_DIR}/${ENVIRONMENT}/config.yaml"
if [ ! -f "$ENV_CONFIG_FILE" ]; then
    print_error "Environment configuration not found: $ENV_CONFIG_FILE"
    exit 1
fi

# Helper function to convert string to uppercase (bash 3 compatible)
to_upper() {
    echo "$1" | tr '[:lower:]' '[:upper:]'
}

# Display deployment banner
clear
print_header "AJASTA MULTI-ENVIRONMENT DEPLOYMENT"
echo -e "${GREEN}Environment:${NC} ${YELLOW}$(to_upper "$ENVIRONMENT")${NC}"
echo -e "${GREEN}Start Step:${NC} ${YELLOW}${START_STEP}${NC}"
if [ -n "$VERBOSITY" ]; then
    echo -e "${GREEN}Verbosity:${NC} ${YELLOW}${VERBOSITY}${NC}"
fi
if [ "$DRY_RUN" = true ]; then
    echo -e "${YELLOW}DRY RUN MODE - No changes will be made${NC}"
fi
echo ""

# Show environment details
print_info "Environment Configuration:"
echo "  - Config File: $ENV_CONFIG_FILE"
echo "  - VM Prefix: ajasta-${ENVIRONMENT}"
echo "  - Terraform Prefix: ajasta-${ENVIRONMENT}"

# Show deployment plan
echo ""
print_info "Deployment Plan:"

if [ "$START_STEP" -le 1 ]; then
    echo "  ${GREEN}✓${NC} Step 1: Generate Ansible inventory from Yandex Cloud"
else
    echo "  ${CYAN}⊘${NC} Step 1: Generate Ansible inventory (skipped)"
fi

if [ "$START_STEP" -le 2 ]; then
    echo "  ${GREEN}✓${NC} Step 2: Setup Kubernetes cluster"
else
    echo "  ${CYAN}⊘${NC} Step 2: Setup Kubernetes cluster (skipped)"
fi

if [ "$START_STEP" -le 3 ]; then
    echo "  ${GREEN}✓${NC} Step 3: Deploy CloudNativePG and PostgreSQL"
else
    echo "  ${CYAN}⊘${NC} Step 3: Deploy CloudNativePG (skipped)"
fi

if [ "$START_STEP" -le 4 ]; then
    echo "  ${GREEN}✓${NC} Step 4: Deploy Ajasta application"
else
    echo "  ${CYAN}⊘${NC} Step 4: Deploy Ajasta application (skipped)"
fi

if [ "$START_STEP" -le 5 ]; then
    echo "  ${GREEN}✓${NC} Step 5: Fix connection timeout issues"
else
    echo "  ${CYAN}⊘${NC} Step 5: Fix connection timeout (skipped)"
fi

echo ""

# Safety check for production
if [ "$ENVIRONMENT" = "production" ] && [ "$START_STEP" -le 2 ] && [ "$SKIP_CONFIRMATION" = false ]; then
    print_warning "You are about to deploy to PRODUCTION environment!"
    echo ""
    echo "This will:"
    echo "  - Generate inventory from production VMs"
    echo "  - Setup/modify Kubernetes cluster"
    echo "  - Deploy application to production"
    echo ""
    read -p "Type 'production' to confirm: " confirmation
    if [ "$confirmation" != "production" ]; then
        print_error "Deployment cancelled"
        exit 1
    fi
    echo ""
fi

# Dry run check
if [ "$DRY_RUN" = true ]; then
    print_info "Dry run mode - would execute the following steps:"
    echo ""
    print_step 1 "Generate Ansible inventory from Yandex Cloud"
    echo "  Command: ${ANSIBLE_K8S_DIR}/generate-inventory-from-yc.sh"
    echo ""
    print_step 2 "Setup Kubernetes cluster"
    echo "  Command: ${ANSIBLE_K8S_DIR}/k8s-cluster-setup.sh ${START_STEP} ${VERBOSITY}"
    echo ""
    print_step 3 "Deploy CloudNativePG and PostgreSQL"
    echo "  Command: ansible-playbook -i inventory.ini 19-deploy-cloudnativepg.yml ${VERBOSITY}"
    echo ""
    print_step 4 "Deploy Ajasta application"
    echo "  Command: ${ANSIBLE_APP_DIR}/deploy-ajasta.sh ${START_STEP} ${VERBOSITY}"
    echo ""
    print_step 5 "Fix connection timeout issues"
    echo "  Command: cd ${ANSIBLE_APP_DIR} && ansible-playbook -i ../k8s/inventory.ini 31-fix-connection-timeout-complete.yml ${VERBOSITY}"
    echo ""
    print_success "Dry run complete"
    exit 0
fi

# Final confirmation
if [ "$SKIP_CONFIRMATION" = false ]; then
    read -p "Proceed with deployment? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        print_error "Deployment cancelled"
        exit 0
    fi
fi

echo ""
print_info "Starting deployment to ${ENVIRONMENT}..."
echo ""

# ==============================================================

# STEP 1: Generate Ansible inventory from Yandex Cloud
# ==============================================================
if [ "$START_STEP" -le 1 ]; then
    print_step 1 "Generate Ansible inventory from Yandex Cloud"

    print_info "Running inventory generation script..."
    cd "${ANSIBLE_K8S_DIR}"

    if bash generate-inventory-from-yc.sh; then
        print_success "Ansible inventory generated successfully"
    else
        print_error "Failed to generate Ansible inventory"
        echo "Check that:"
        echo "  1. Yandex Cloud CLI (yc) is installed and authenticated"
        echo "  2. VMs are running in Yandex Cloud"
        echo "  3. VMs have prefix: ajasta-${ENVIRONMENT}"
        exit 1
    fi
else
    print_step 1 "Generate Ansible inventory from Yandex Cloud"
    print_info "Skipping (step 1 < START_STEP)"
fi

# ==============================================================
# STEP 2: Setup Kubernetes cluster
# ==============================================================
if [ "$START_STEP" -le 2 ]; then
    print_step 2 "Setup Kubernetes cluster"

    print_info "Running k8s-cluster-setup script..."
    cd "${ANSIBLE_K8S_DIR}"

    if bash k8s-cluster-setup.sh 1 ${VERBOSITY}; then
        print_success "Kubernetes cluster setup completed"
    else
        print_error "Kubernetes cluster setup failed"
        exit 1
    fi
else
    print_step 2 "Setup Kubernetes cluster"
    print_info "Skipping (step 2 < START_STEP)"
fi

# ==============================================================
# STEP 3: Deploy CloudNativePG and PostgreSQL cluster
# ==============================================================
if [ "$START_STEP" -le 3 ]; then
    print_step 3 "Deploy CloudNativePG and PostgreSQL cluster"

    print_info "Deploying CloudNativePG operator..."
    cd "${ANSIBLE_K8S_DIR}"

    if ansible-playbook -i inventory.ini 19-deploy-cloudnativepg.yml ${VERBOSITY}; then
        print_success "CloudNativePG and PostgreSQL deployed successfully"
    else
        print_error "CloudNativePG deployment failed"
        exit 1
    fi
else
    print_step 3 "Deploy CloudNativePG and PostgreSQL cluster"
    print_info "Skipping (step 3 < START_STEP)"
fi

# ==============================================================
# STEP 4: Deploy Ajasta application
# ==============================================================
if [ "$START_STEP" -le 4 ]; then
    print_step 4 "Deploy Ajasta application"

    print_info "Running deploy-ajasta script..."
    cd "${ANSIBLE_APP_DIR}"

    # Pass environment to deploy-ajasta.sh
    export DEPLOY_ENVIRONMENT="${ENVIRONMENT}"

    if bash deploy-ajasta.sh 1 ${VERBOSITY}; then
        print_success "Ajasta application deployed successfully"
    else
        print_error "Ajasta application deployment failed"
        exit 1
    fi
else
    print_step 4 "Deploy Ajasta application"
    print_info "Skipping (step 4 < START_STEP)"
fi

# ==============================================================
# STEP 5: Fix connection timeout issues
# ==============================================================
if [ "$START_STEP" -le 5 ]; then
    print_step 5 "Fix connection timeout issues"

    print_info "Running connection timeout fix playbook..."
    cd "${ANSIBLE_APP_DIR}"

    if ansible-playbook -i ../k8s/inventory.ini 31-fix-connection-timeout-complete.yml ${VERBOSITY}; then
        print_success "Connection timeout issues resolved"
    else
        print_warning "Connection timeout fix completed with warnings"
        echo "This is often non-critical. Check the output above."
    fi
else
    print_step 5 "Fix connection timeout issues"
    print_info "Skipping (step 5 < START_STEP)"
fi

# ==============================================================
# DEPLOYMENT COMPLETE
# ==============================================================
print_header "DEPLOYMENT COMPLETE"

echo ""
echo -e "${GREEN}✓ Deployment to ${YELLOW}$(to_upper "$ENVIRONMENT")${GREEN} completed successfully!${NC}"
echo ""

# Show environment-specific information
if [ "$ENVIRONMENT" = "staging" ]; then
    echo "Staging Environment:"
    echo "  - URL: https://staging.ajasta.top"
    echo "  - Backend API: https://staging.ajasta.top/api"
    echo ""
else
    echo "Production Environment:"
    echo "  - URL: https://ajasta.top"
    echo "  - Backend API: https://ajasta.top/api"
    echo ""
fi

echo "Next Steps:"
echo "  1. Verify application is accessible"
echo "  2. Check pod status: kubectl get pods -n ajasta-${ENVIRONMENT}"
echo "  3. View logs: kubectl logs -n ajasta-${ENVIRONMENT} -l component=backend -f"
echo "  4. Monitor ingress: kubectl get ingress -n ajasta-${ENVIRONMENT}"
echo ""

print_success "All done!"
echo ""
