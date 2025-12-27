#!/usr/bin/env bash
# Quick Bootstrap Script - Automated K8s bootstrap and app deployment
# This script orchestrates the complete workflow

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ANSIBLE_CI_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
PROJECT_ROOT="$(cd "${ANSIBLE_CI_DIR}/.." && pwd)"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
INVENTORY_FILE="${ANSIBLE_CI_DIR}/inventory.ini"
INVENTORY_SCRIPT="${ANSIBLE_CI_DIR}/scripts/generate-inventory-from-terraform.sh"
BOOTSTRAP_PLAYBOOK="${ANSIBLE_CI_DIR}/k8s-bootstrap.yml"
DEPLOY_PLAYBOOK="${ANSIBLE_CI_DIR}/deploy-apps.yml"
STATUS_PLAYBOOK="${ANSIBLE_CI_DIR}/status.yml"

# Functions
log_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

log_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

log_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

log_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_header() {
    echo ""
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BLUE}  $1${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""
}

# Check prerequisites
check_prerequisites() {
    print_header "Checking Prerequisites"

    # Check required commands
    local required_commands=("ansible" "ansible-playbook" "jq" "kubectl" "helm")
    for cmd in "${required_commands[@]}"; do
        if ! command -v "$cmd" &> /dev/null; then
            log_error "Missing required command: $cmd"
            echo "Install with: brew install $cmd (macOS) or apt-get install $cmd (Linux)"
            exit 1
        fi
        log_success "$cmd is installed"
    done

    # Check Terraform outputs
    if [[ ! -f "${PROJECT_ROOT}/terraform/outputs.json" ]]; then
        log_error "Terraform outputs not found: ${PROJECT_ROOT}/terraform/outputs.json"
        echo "Please run 'terraform apply' in the terraform/ directory first"
        exit 1
    fi
    log_success "Terraform outputs found"

    # Check Python dependencies
    if ! python3 -c "import kubernetes.core" 2>/dev/null; then
        log_warning "Kubernetes Ansible collection not installed"
        echo "Installing: ansible-galaxy collection install kubernetes.core"
        ansible-galaxy collection install kubernetes.core
    fi
    log_success "Python dependencies OK"
}

# Generate inventory
generate_inventory() {
    print_header "Generating Ansible Inventory"

    if [[ ! -f "${INVENTORY_SCRIPT}" ]]; then
        log_error "Inventory script not found: ${INVENTORY_SCRIPT}"
        exit 1
    fi

    log_info "Running inventory generation script..."
    bash "${INVENTORY_SCRIPT}"

    if [[ ! -f "${INVENTORY_FILE}" ]]; then
        log_error "Inventory file not generated: ${INVENTORY_FILE}"
        exit 1
    fi

    log_success "Inventory generated: ${INVENTORY_FILE}"
}

# Test connectivity
test_connectivity() {
    print_header "Testing SSH Connectivity"

    log_info "Testing master node..."
    if ansible k8s-master -i "${INVENTORY_FILE}" -m ping &> /dev/null; then
        log_success "Master node reachable"
    else
        log_error "Cannot connect to master node"
        echo "Check:"
        echo "  1. VMs are running: yc compute instance list"
        echo "  2. SSH key is configured"
        echo "  3. Firewall allows SSH (port 22)"
        exit 1
    fi

    log_info "Testing all nodes..."
    if ansible k8s -i "${INVENTORY_FILE}" -m ping &> /dev/null; then
        log_success "All nodes reachable"
    else
        log_warning "Some nodes unreachable (may be OK if workers don't have public IPs)"
    fi
}

# Bootstrap Kubernetes
bootstrap_k8s() {
    print_header "Bootstrapping Kubernetes Cluster"

    log_info "This will take 10-20 minutes..."
    echo "Press Ctrl+C to cancel"
    echo ""

    sleep 3

    if ansible-playbook -i "${INVENTORY_FILE}" "${BOOTSTRAP_PLAYBOOK}"; then
        log_success "Kubernetes bootstrap complete"
    else
        log_error "Kubernetes bootstrap failed"
        exit 1
    fi
}

# Deploy applications
deploy_apps() {
    print_header "Deploying Applications"

    # Check for required environment variables
    if [[ -z "${POSTGRES_PASSWORD:-}" ]]; then
        log_warning "POSTGRES_PASSWORD not set"
        read -p "Enter PostgreSQL password: " -s POSTGRES_PASSWORD
        export POSTGRES_PASSWORD
        echo ""
    fi

    if [[ -z "${JWT_SECRET:-}" ]]; then
        log_warning "JWT_SECRET not set"
        read -p "Enter JWT secret: " -s JWT_SECRET
        export JWT_SECRET
        echo ""
    fi

    log_info "Deploying applications..."

    if ansible-playbook -i "${INVENTORY_FILE}" "${DEPLOY_PLAYBOOK}"; then
        log_success "Application deployment complete"
    else
        log_error "Application deployment failed"
        exit 1
    fi
}

# Show status
show_status() {
    print_header "Cluster and Application Status"

    ansible-playbook -i "${INVENTORY_FILE}" "${STATUS_PLAYBOOK}"
}

# Main workflow
main() {
    clear

    print_header "Ansible CI - Quick Bootstrap"
    echo "This script will:"
    echo "  1. Check prerequisites"
    echo "  2. Generate inventory from Terraform"
    echo "  3. Test SSH connectivity"
    echo "  4. Bootstrap Kubernetes cluster"
    echo "  5. Deploy Ajasta applications"
    echo "  6. Show cluster status"
    echo ""
    read -p "Continue? (y/N) " -n 1 -r
    echo ""

    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        log_info "Cancelled by user"
        exit 0
    fi

    # Execute workflow
    check_prerequisites
    generate_inventory
    test_connectivity

    # Ask if user wants to bootstrap K8s
    echo ""
    read -p "Bootstrap Kubernetes cluster? (Y/n) " -n 1 -r
    echo ""
    if [[ ! $REPLY =~ ^[Nn]$ ]]; then
        bootstrap_k8s
    else
        log_info "Skipping Kubernetes bootstrap"
    fi

    # Ask if user wants to deploy apps
    echo ""
    read -p "Deploy applications? (Y/n) " -n 1 -r
    echo ""
    if [[ ! $REPLY =~ ^[Nn]$ ]]; then
        deploy_apps
    else
        log_info "Skipping application deployment"
    fi

    # Show status
    show_status

    print_header "Bootstrap Complete!"
    log_success "Your Kubernetes cluster is ready!"
    echo ""
    echo "Next steps:"
    echo "  1. Access the cluster: ssh ajasta@<master-ip>"
    echo "  2. Check pods: kubectl get pods -n ajasta"
    echo "  3. View logs: kubectl logs -n ajasta -l app=ajasta-backend -f"
    echo ""
}

# Run main
main "$@"
