#!/usr/bin/env bash
# Complete workflow: Fetch fresh Terraform outputs and generate Ansible inventory
# This script orchestrates the entire process to ensure inventory is always fresh

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

log_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

log_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

log_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_header() {
    clear
    echo ""
    echo -e "${BLUE}╔════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║  $1${NC}"
    echo -e "${BLUE}╚════════════════════════════════════════════════════════════╝${NC}"
    echo ""
}

# Check environment variable
check_gitlab_pat() {
    if [[ -z "${GITLAB_PAT:-}" ]] || [[ "${GITLAB_PAT}" == "glpat-" ]]; then
        log_error "GITLAB_PAT environment variable not set"
        echo ""
        echo "Please set your GitLab Personal Access Token:"
        echo "  export GITLAB_PAT=\"glpat-your-token-here\""
        echo ""
        echo "Create token at: https://otusteam.gitlab.yandexcloud.net/-/user_settings/personal_access_tokens"
        echo "Required scope: api"
        exit 1
    fi
    log_success "GitLab PAT is configured"
}

# Step 1: Fetch fresh Terraform outputs
fetch_outputs() {
    print_header "Step 1: Fetching Fresh Terraform Outputs"

    local environment="${1:-production}"

    log_info "Environment: ${environment}"
    echo ""

    if [[ ! -f "${PROJECT_ROOT}/scripts/get-terraform-outputs-from-gitlab.sh" ]]; then
        log_error "get-terraform-outputs-from-gitlab.sh not found"
        exit 1
    fi

    # Run the fetch script with 'y' input
    cd "${PROJECT_ROOT}"
    echo "y" | ./scripts/get-terraform-outputs-from-gitlab.sh "${environment}" 2>&1 | head -100

    if [[ ${PIPESTATUS[0]} -eq 0 ]]; then
        log_success "Terraform outputs fetched successfully"
    else
        log_error "Failed to fetch Terraform outputs"
        exit 1
    fi
}

# Step 2: Verify outputs against actual infrastructure
verify_outputs() {
    print_header "Step 2: Verifying Outputs Against Actual Infrastructure"

    if ! command -v yc &> /dev/null; then
        log_warning "YC CLI not found, skipping verification"
        return
    fi

    log_info "Fetching actual VM list from Yandex Cloud..."
    echo ""

    # Get actual IPs from Yandex Cloud
    ACTUAL_MASTER_IP=$(yc compute instance list --format yaml | grep -A 5 "name: k8s-master" | grep "oneof_ipv4_address:" | awk '{print $2}' || echo "")

    if [[ -n "${ACTUAL_MASTER_IP}" ]]; then
        log_success "Actual master IP: ${ACTUAL_MASTER_IP}"

        # Compare with outputs.json
        OUTPUTS_MASTER_IP=$(jq -r '.master_public_ip' "${PROJECT_ROOT}/terraform/outputs.json")

        if [[ "${ACTUAL_MASTER_IP}" == "${OUTPUTS_MASTER_IP}" ]]; then
            log_success "✓ Outputs match actual infrastructure"
        else
            log_error "✗ Outputs mismatch!"
            echo "  Actual master IP:    ${ACTUAL_MASTER_IP}"
            echo "  Outputs master IP:   ${OUTPUTS_MASTER_IP}"
            echo ""
            log_warning "GitLab state is stale! Terraform apply needs to be run."
            return 1
        fi
    else
        log_warning "Could not verify master IP (YC CLI issue?)"
    fi
}

# Step 3: Generate Ansible inventory
generate_inventory() {
    print_header "Step 3: Generating Ansible Inventory"

    local inventory_script="${SCRIPT_DIR}/generate-inventory-from-terraform.sh"

    if [[ ! -f "${inventory_script}" ]]; then
        log_error "generate-inventory-from-terraform.sh not found"
        exit 1
    fi

    # Make executable
    chmod +x "${inventory_script}"

    # Run inventory generation
    cd "${ANSIBLE_CI_DIR:-${PROJECT_ROOT}/ansible-ci}"

    if bash "${inventory_script}" 2>&1; then
        log_success "Inventory generated and verified"
        return 0
    else
        log_error "Inventory generation failed (but inventory file created)"
        return 1
    fi
}

# Main workflow
main() {
    print_header "Complete Inventory Update Workflow"

    local environment="${1:-production}"

    echo "This script will:"
    echo "  1. Fetch fresh Terraform outputs from GitLab (${environment})"
    echo "  2. Verify outputs match actual infrastructure"
    echo "  3. Generate Ansible inventory with connectivity testing"
    echo ""
    read -p "Continue? (y/N) " -n 1 -r
    echo ""

    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        log_info "Cancelled by user"
        exit 0
    fi

    # Check prerequisites
    check_gitlab_pat

    # Execute workflow
    fetch_outputs "${environment}"

    if ! verify_outputs; then
        echo ""
        log_warning "Verification failed, but continuing with inventory generation..."
        echo "You may need to run terraform:apply in GitLab CI/CD first."
        echo ""
        read -p "Continue anyway? (y/N) " -n 1 -r
        echo ""
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            log_info "Cancelled by user"
            exit 1
        fi
    fi

    if generate_inventory; then
        echo ""
        print_header "Success!"
        log_success "Inventory is now fresh and verified!"
        echo ""
        echo "Next steps:"
        echo "  cd ${PROJECT_ROOT}/ansible-ci"
        echo "  ansible k8s -i inventory.ini -m ping"
        exit 0
    else
        echo ""
        log_warning "Inventory generated with connectivity issues"
        exit 1
    fi
}

# Run main
main "$@"
