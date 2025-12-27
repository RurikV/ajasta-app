#!/usr/bin/env bash
# Generate Ansible inventory from Terraform outputs (GitLab or local)
# This script automatically detects and fetches outputs from GitLab or local file

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
ANSIBLE_CI_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
TERRAFORM_DIR="${PROJECT_ROOT}/terraform"
OUTPUTS_FILE="${TERRAFORM_DIR}/outputs.json"
INVENTORY_FILE="${ANSIBLE_CI_DIR}/inventory.ini"
FETCH_SCRIPT="${PROJECT_ROOT}/scripts/get-terraform-outputs-from-gitlab.sh"

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

# Try to fetch outputs from GitLab if local file doesn't exist
fetch_from_gitlab_if_needed() {
    if [[ ! -f "${OUTPUTS_FILE}" ]]; then
        print_header "Local Terraform Outputs Not Found"

        log_warning "Terraform outputs file not found locally"
        log_info "Attempting to fetch from GitLab..."

        if [[ ! -f "${FETCH_SCRIPT}" ]]; then
            log_error "GitLab fetch script not found: ${FETCH_SCRIPT}"
            echo ""
            echo "Please run Terraform manually to generate outputs.json:"
            echo "  1. cd ${TERRAFORM_DIR}"
            echo "  2. terraform init -reconfigure"
            echo "  3. terraform apply"
            echo ""
            exit 1
        fi

        # Ask user which environment to fetch
        echo ""
        echo "Which environment's outputs do you want to fetch?"
        echo "  1) production (default)"
        echo "  2) staging"
        echo "  3) cancel"
        echo ""
        read -p "Select environment [1-3]: " -n 1 -r
        echo ""

        case $REPLY in
            2)
                ENV="staging"
                ;;
            3)
                log_info "Cancelled by user"
                exit 0
                ;;
            *)
                ENV="production"
                ;;
        esac

        log_info "Fetching outputs from GitLab (environment: ${ENV})..."

        # Run the fetch script
        if bash "${FETCH_SCRIPT}" "${ENV}"; then
            log_success "Successfully fetched outputs from GitLab"
        else
            log_error "Failed to fetch outputs from GitLab"
            echo ""
            echo "Make sure:"
            echo "  1. GITLAB_PAT environment variable is set"
            echo "  2. Token has 'api' scope"
            echo "  3. Terraform was applied via GitLab CI/CD for ${ENV}"
            echo ""
            exit 1
        fi
    else
        log_success "Using local Terraform outputs: ${OUTPUTS_FILE}"
    fi
}

# Generate inventory from outputs
generate_inventory() {
    print_header "Generating Ansible Inventory"

    # Check if jq is installed
    if ! command -v jq &> /dev/null; then
        log_error "jq is required but not installed"
        echo "Install with: brew install jq (macOS) or apt-get install jq (Linux)"
        exit 1
    fi

    # Check if outputs file exists
    if [[ ! -f "${OUTPUTS_FILE}" ]]; then
        log_error "Terraform outputs file not found: ${OUTPUTS_FILE}"
        exit 1
    fi

    # Read Terraform outputs (handle both GitLab format with .value and direct format)
    MASTER_IP=$(jq -r '.master_public_ip | if type == "object" and has("value") then .value elif type == "string" then . else . end' "${OUTPUTS_FILE}")
    WORKER_IPS=$(jq -r '.worker_public_ips | if type == "object" and has("value") then .value elif type == "object" then . else . end' "${OUTPUTS_FILE}")

    # Validate master IP
    if [[ -z "${MASTER_IP}" ]] || [[ "${MASTER_IP}" == "null" ]]; then
        log_error "Could not extract master_public_ip from Terraform outputs"
        echo "Contents of outputs.json:"
        jq '.' "${OUTPUTS_FILE}"
        exit 1
    fi

    # Create inventory file
    cat > "${INVENTORY_FILE}" << EOF
# Ansible inventory generated from Terraform outputs
# Generated at: $(date -u +"%Y-%m-%d %H:%M:%S UTC")
# DO NOT EDIT MANUALLY - Use generate-inventory-from-terraform.sh to regenerate

[k8s-master]
k8s-master ansible_host=${MASTER_IP}

[k8s-workers]
EOF

    # Add workers to inventory
    WORKER_COUNT=0
    if [[ -n "${WORKER_IPS}" ]] && [[ "${WORKER_IPS}" != "null" ]] && [[ "${WORKER_IPS}" != "{}" ]]; then
        # Parse worker IPs from JSON object
        for key in $(echo "${WORKER_IPS}" | jq -r 'keys[]'); do
            WORKER_IP=$(echo "${WORKER_IPS}" | jq -r ".[\"${key}\"]")
            if [[ -n "${WORKER_IP}" ]] && [[ "${WORKER_IP}" != "null" ]]; then
                echo "k8s-worker-${WORKER_COUNT} ansible_host=${WORKER_IP}" >> "${INVENTORY_FILE}"
                WORKER_COUNT=$((WORKER_COUNT + 1))
            fi
        done
    fi

    # Add k8s group (all nodes)
    cat >> "${INVENTORY_FILE}" << EOF

[k8s:children]
k8s-master
k8s-workers
EOF

    log_success "Ansible inventory generated successfully!"
}

# Display summary
display_summary() {
    print_header "Inventory Summary"

    echo "Cluster nodes:"
    echo "  Master:  ${MASTER_IP}"
    if [[ ${WORKER_COUNT} -gt 0 ]]; then
        echo "  Workers: ${WORKER_COUNT} node(s)"
        grep "k8s-worker" "${INVENTORY_FILE}" | sed 's/^/    /'
    else
        echo "  Workers: (none - master-only cluster)"
    fi
    echo ""
    echo "Inventory file: ${INVENTORY_FILE}"
    echo ""
    echo "Test connectivity with:"
    echo "  ansible k8s-master -i ${INVENTORY_FILE} -m ping"
    echo "  ansible k8s -i ${INVENTORY_FILE} -m ping"
}

# Main workflow
main() {
    clear

    print_header "Ansible Inventory Generator (GitLab/Local)"
    echo "This script generates Ansible inventory from Terraform outputs"
    echo "It will automatically try GitLab if local outputs are not available"
    echo ""

    fetch_from_gitlab_if_needed
    generate_inventory
    display_summary

    print_header "Ready to Bootstrap!"
    log_success "Inventory generated successfully!"
    echo ""
    echo "Next steps:"
    echo "  1. Test connectivity:"
    echo "     ansible k8s-master -i ${INVENTORY_FILE} -m ping"
    echo ""
    echo "  2. Bootstrap Kubernetes:"
    echo "     ansible-playbook -i ${INVENTORY_FILE} k8s-bootstrap.yml"
    echo ""
    echo "  3. Deploy applications:"
    echo "     ansible-playbook -i ${INVENTORY_FILE} deploy-apps.yml"
    echo ""
}

# Run main
main "$@"
