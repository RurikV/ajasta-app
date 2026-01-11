#!/usr/bin/env bash
# Generate Ansible inventory from Terraform outputs with validation
# This script reads terraform/outputs.json and creates ansible-ci/inventory.ini
# It validates connectivity and warns about stale data

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
TERRAFORM_DIR="${PROJECT_ROOT}/terraform"
ANSIBLE_CI_DIR="${PROJECT_ROOT}/ansible-ci"
OUTPUTS_FILE="${TERRAFORM_DIR}/outputs.json"
INVENTORY_FILE="${ANSIBLE_CI_DIR}/inventory.ini"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Counters
SUCCESS_COUNT=0
FAILED_COUNT=0

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

    # Check for jq
    if ! command -v jq &> /dev/null; then
        log_error "jq is required but not installed"
        echo "Install with: brew install jq (macOS) or apt-get install jq (Linux)"
        exit 1
    fi
    log_success "jq is installed"

    # Check for ansible
    if ! command -v ansible &> /dev/null; then
        log_warning "ansible is not installed (optional, for connectivity testing)"
    else
        log_success "ansible is installed"
    fi

    # Check if outputs.json exists
    if [[ ! -f "${OUTPUTS_FILE}" ]]; then
        log_error "Terraform outputs file not found: ${OUTPUTS_FILE}"
        echo ""
        echo "Please generate Terraform outputs first:"
        echo "  cd ${PROJECT_ROOT}"
        echo "  ./scripts/get-terraform-outputs-from-gitlab.sh production"
        exit 1
    fi

    # Check outputs.json modification time
    OUTPUTS_AGE=$(($(date +%s) - $(stat -f %m "${OUTPUTS_FILE}" 2>/dev/null || stat -c %Y "${OUTPUTS_FILE}" 2>/dev/null)))
    OUTPUTS_AGE_MINUTES=$((OUTPUTS_AGE / 60))

    if [[ ${OUTPUTS_AGE_MINUTES} -gt 60 ]]; then
        log_warning "outputs.json is ${OUTPUTS_AGE_MINUTES} minutes old (might be stale)"
        echo "  Consider refreshing with: ./scripts/get-terraform-outputs-from-gitlab.sh production"
    else
        log_success "outputs.json is recent (${OUTPUTS_AGE_MINUTES} minutes old)"
    fi
}

# Read and validate Terraform outputs
read_terraform_outputs() {
    print_header "Reading Terraform Outputs"

    # Read Terraform outputs (handle both GitLab format with .value and direct format)
    MASTER_IP=$(jq -r '.master_public_ip // empty' "${OUTPUTS_FILE}")

    # Try to extract from .value if needed (GitLab HTTP backend format)
    if [[ -z "${MASTER_IP}" ]] || [[ "${MASTER_IP}" == "null" ]]; then
        MASTER_IP=$(jq -r '.master_public_ip.value // empty' "${OUTPUTS_FILE}")
    fi

    WORKER_IPS=$(jq -r '.worker_public_ips // empty' "${OUTPUTS_FILE}")

    # Try to extract from .value if needed
    if [[ -z "${WORKER_IPS}" ]] || [[ "${WORKER_IPS}" == "null" ]]; then
        WORKER_IPS=$(jq -r '.worker_public_ips.value // empty' "${OUTPUTS_FILE}")
    fi

    # Validate master IP
    if [[ -z "${MASTER_IP}" ]] || [[ "${MASTER_IP}" == "null" ]]; then
        log_error "Could not extract master_public_ip from Terraform outputs"
        echo ""
        echo "Contents of outputs.json (first 20 lines):"
        head -20 "${OUTPUTS_FILE}"
        exit 1
    fi

    log_success "Master IP: ${MASTER_IP}"

    # Parse workers
    WORKER_LIST=()
    if [[ -n "${WORKER_IPS}" ]] && [[ "${WORKER_IPS}" != "null" ]] && [[ "${WORKER_IPS}" != "{}" ]]; then
        # Parse worker IPs from JSON object
        WORKER_COUNT=$(echo "${WORKER_IPS}" | jq 'length')
        log_info "Found ${WORKER_COUNT} worker(s) in outputs"

        for i in $(seq 0 $((WORKER_COUNT - 1))); do
            WORKER_KEY=$(echo "${WORKER_IPS}" | jq -r "keys[$i]")
            WORKER_IP=$(echo "${WORKER_IPS}" | jq -r ".[\"${WORKER_KEY}\"]")
            if [[ -n "${WORKER_IP}" ]] && [[ "${WORKER_IP}" != "null" ]]; then
                WORKER_LIST+=("${WORKER_IP}")
                log_info "  Worker ${i}: ${WORKER_IP}"
            fi
        done
    else
        log_warning "No worker IPs found (master-only cluster)"
    fi
}

# Get SSH configuration
get_ssh_config() {
    print_header "SSH Configuration"

    # Read SSH username from group_vars
    GROUP_VARS_FILE="${ANSIBLE_CI_DIR}/group_vars/all.yml"
    if [[ -f "${GROUP_VARS_FILE}" ]]; then
        # Use python/yq for reliable YAML parsing
        if command -v python3 &> /dev/null; then
            SSH_USER=$(python3 -c "import yaml; print(yaml.safe_load(open('${GROUP_VARS_FILE}')).get('ssh_username', 'ajasta'))" 2>/dev/null || echo "ajasta")
            SSH_KEY=$(python3 -c "import yaml; print(yaml.safe_load(open('${GROUP_VARS_FILE}')).get('ssh_private_key_file', ''))" 2>/dev/null || echo "")
        else
            # Fallback to grep
            SSH_USER=$(grep "^ssh_username:" "${GROUP_VARS_FILE}" | awk '{print $2}' | tr -d '"' || echo "ajasta")
            SSH_KEY=$(grep "^ssh_private_key_file:" "${GROUP_VARS_FILE}" | awk '{print $2}' | tr -d '"' || echo "")
        fi
    else
        SSH_USER="ajasta"
        SSH_KEY=""
        log_warning "group_vars/all.yml not found, using defaults"
    fi

    log_success "SSH user: ${SSH_USER}"

    # Auto-detect SSH key if not specified
    if [[ -z "${SSH_KEY}" ]] || [[ ! -f "${SSH_KEY}" ]]; then
        log_info "Auto-detecting SSH key..."
        for key in "$HOME/.ssh/id_rsa_k8s" "$HOME/.ssh/id_ed25519" "$HOME/.ssh/id_rsa"; do
            if [[ -f "${key}" ]]; then
                SSH_KEY="${key}"
                log_success "Found SSH key: ${SSH_KEY}"
                break
            fi
        done
    fi

    if [[ -n "${SSH_KEY}" ]] && [[ -f "${SSH_KEY}" ]]; then
        log_success "Using SSH key: ${SSH_KEY}"
    else
        log_warning "No SSH key found, will use default SSH agent or password auth"
        SSH_KEY=""
    fi
}

# Generate inventory file
generate_inventory() {
    print_header "Generating Ansible Inventory"

    # Create inventory file with proper naming (avoid group/host name conflict)
    cat > "${INVENTORY_FILE}" << EOF
# Ansible inventory generated from Terraform outputs
# Generated at: $(date -u +"%Y-%m-%d %H:%M:%S UTC")
# DO NOT EDIT MANUALLY - Use generate-inventory-from-terraform.sh to regenerate

[cluster_master]
master-node ansible_host=${MASTER_IP} ansible_user=${SSH_USER}$(if [[ -n "${SSH_KEY}" ]]; then echo " ansible_ssh_private_key_file=${SSH_KEY}"; fi)

[cluster_workers]
EOF

    # Add workers
    WORKER_INDEX=0
    for WORKER_IP in "${WORKER_LIST[@]}"; do
        echo "worker-node-${WORKER_INDEX} ansible_host=${WORKER_IP} ansible_user=${SSH_USER}$(if [[ -n "${SSH_KEY}" ]]; then echo " ansible_ssh_private_key_file=${SSH_KEY}"; fi)" >> "${INVENTORY_FILE}"
        WORKER_INDEX=$((WORKER_INDEX + 1))
    done

    # Add group aliases
    cat >> "${INVENTORY_FILE}" << EOF

[k8s_master:children]
cluster_master

[k8s_workers:children]
cluster_workers

[k8s:children]
k8s_master
k8s_workers
EOF

    log_success "Inventory generated: ${INVENTORY_FILE}"
    echo ""
    echo "Cluster topology:"
    echo "  Master:  ${MASTER_IP}"
    if [[ ${WORKER_INDEX} -gt 0 ]]; then
        echo "  Workers: ${WORKER_INDEX} node(s)"
        for i in $(seq 0 $((WORKER_INDEX - 1))); do
            WORKER_IP=$(echo "${WORKER_LIST[$i]}")
            echo "    worker-node-${i}: ${WORKER_IP}"
        done
    else
        echo "  Workers: (none - master-only cluster)"
    fi
}

# Test connectivity
test_connectivity() {
    print_header "Testing Connectivity"

    if ! command -v ansible &> /dev/null; then
        log_warning "ansible not installed, skipping connectivity tests"
        echo "Install ansible to test: brew install ansible"
        return
    fi

    log_info "Testing SSH connectivity to cluster nodes..."

    # Test master
    echo -n "  Testing master-node (${MASTER_IP})... "
    if timeout 10 ansible master-node -i "${INVENTORY_FILE}" -m ping &> /dev/null; then
        echo -e "${GREEN}✓ REACHABLE${NC}"
        SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
    else
        echo -e "${RED}✗ UNREACHABLE${NC}"
        FAILED_COUNT=$((FAILED_COUNT + 1))
        log_warning "Master node is not reachable"
        echo "  Possible causes:"
        echo "    1. VM is not running (check: yc compute instance list)"
        echo "    2. Wrong IP in outputs.json (stale data)"
        echo "    3. SSH key is incorrect"
        echo "    4. Firewall is blocking SSH (port 22)"
    fi

    # Test workers
    for i in $(seq 0 $((${#WORKER_LIST[@]} - 1))); do
        WORKER_IP="${WORKER_LIST[$i]}"
        echo -n "  Testing worker-node-${i} (${WORKER_IP})... "
        if timeout 10 ansible "worker-node-${i}" -i "${INVENTORY_FILE}" -m ping &> /dev/null; then
            echo -e "${GREEN}✓ REACHABLE${NC}"
            SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
        else
            echo -e "${RED}✗ UNREACHABLE${NC}"
            FAILED_COUNT=$((FAILED_COUNT + 1))
        fi
    done

    echo ""
    echo "Connectivity summary:"
    echo "  ✓ Reachable: ${SUCCESS_COUNT}"
    if [[ ${FAILED_COUNT} -gt 0 ]]; then
        echo "  ✗ Unreachable: ${FAILED_COUNT}"
        echo ""
        log_warning "Some nodes are unreachable!"
        echo ""
        echo "If IPs are wrong, refresh Terraform outputs:"
        echo "  cd ${PROJECT_ROOT}"
        echo "  ./scripts/get-terraform-outputs-from-gitlab.sh production"
        echo ""
        echo "Then check actual VMs:"
        echo "  yc compute instance list"
    else
        log_success "All nodes are reachable!"
    fi
}

# Show next steps
show_next_steps() {
    print_header "Next Steps"

    echo "Inventory file: ${INVENTORY_FILE}"
    echo ""
    echo "Test connectivity manually:"
    echo "  cd ${ANSIBLE_CI_DIR}"
    echo "  ansible k8s -i ${INVENTORY_FILE} -m ping"
    echo ""
    echo "Install Kubernetes:"
    echo "  ansible-playbook -i ${INVENTORY_FILE} k8s-install.yml"
    echo ""
    echo "Or deploy applications:"
    echo "  ansible-playbook -i ${INVENTORY_FILE} deploy-apps.yml"
}

# Main workflow
main() {
    clear

    print_header "Generate Ansible Inventory from Terraform Outputs"
    echo "This script generates Ansible inventory from Terraform outputs"
    echo "and validates SSH connectivity to all nodes."
    echo ""

    check_prerequisites
    read_terraform_outputs
    get_ssh_config
    generate_inventory

    if [[ ${WORKER_INDEX} -eq 0 ]]; then
        echo ""
        log_warning "Master-only cluster detected"
    fi

    test_connectivity
    show_next_steps

    # Exit with error if any nodes were unreachable
    if [[ ${FAILED_COUNT} -gt 0 ]]; then
        echo ""
        log_warning "Inventory generated but some nodes are unreachable"
        exit 1
    fi

    echo ""
    log_success "Inventory generated and verified successfully!"
    exit 0
}

# Run main
main "$@"
