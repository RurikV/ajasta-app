#!/usr/bin/env bash
# Generate Ansible inventory directly from Yandex Cloud VMs
# Fallback method when Terraform outputs are stale or unavailable

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
ANSIBLE_CI_DIR="${PROJECT_ROOT}/ansible/k8s"
INVENTORY_FILE="${ANSIBLE_CI_DIR}/inventory.ini"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

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

    # Check for yc CLI
    if ! command -v yc &> /dev/null; then
        log_error "Yandex Cloud CLI (yc) is required but not installed"
        echo "Install from: https://cloud.yandex.com/docs/cli/quickstart"
        exit 1
    fi
    log_success "Yandex Cloud CLI is installed"

    # Check if user is authenticated
    if ! yc config get token &> /dev/null; then
        log_error "Not authenticated with Yandex Cloud"
        echo "Run: yc init"
        exit 1
    fi
    log_success "Authenticated with Yandex Cloud"

    # Check for jq
    if ! command -v jq &> /dev/null; then
        log_error "jq is required but not installed"
        echo "Install with: brew install jq"
        exit 1
    fi
    log_success "jq is installed"
}

# Get VM IPs from Yandex Cloud
get_vm_ips() {
    print_header "Fetching VM IPs from Yandex Cloud"

    log_info "Getting list of running Kubernetes VMs..."

    # Get all VMs in JSON format
    VM_LIST=$(yc compute instance list --format json 2>/dev/null || echo "[]")

    VM_COUNT=$(echo "${VM_LIST}" | jq 'length')

    if [[ ${VM_COUNT} -eq 0 ]]; then
        log_error "No VMs found in Yandex Cloud"
        exit 1
    fi

    log_success "Found ${VM_COUNT} VM(s)"

    # Debug: Show all VM names found
    log_info "VM names found:"
    echo "${VM_LIST}" | jq -r '.[].name' | while read -r name; do
        echo "  - ${name}"
    done

    # Extract master IP (use public IP from one-to-one NAT)
    # Try both naming conventions: master-node and k8s-master
    MASTER_IP=$(echo "${VM_LIST}" | jq -r '.[] | select(.name == "master-node" or .name == "k8s-master") | .network_interfaces[0].primary_v4_address.one_to_one_nat.address // empty' | head -1)

    if [[ -z "${MASTER_IP}" ]] || [[ "${MASTER_IP}" == "null" ]]; then
        log_error "Could not find master-node or k8s-master VM public IP"
        exit 1
    fi

    log_success "Master IP: ${MASTER_IP}"

    # Extract worker IPs (use public IP from one-to-one NAT)
    # Try both naming conventions: worker-node-* and k8s-worker-*
    WORKER_LIST=()
    WORKER_IPS=$(echo "${VM_LIST}" | jq -r '.[] | select(.name | startswith("worker-node") or startswith("k8s-worker")) | .network_interfaces[0].primary_v4_address.one_to_one_nat.address // empty' | sort -V)

    for WORKER_IP in ${WORKER_IPS}; do
        if [[ -n "${WORKER_IP}" ]] && [[ "${WORKER_IP}" != "null" ]]; then
            WORKER_LIST+=("${WORKER_IP}")
            log_info "  Worker IP: ${WORKER_IP}"
        fi
    done

    if [[ ${#WORKER_LIST[@]} -eq 0 ]]; then
        log_warning "No worker VMs found (master-only cluster)"
    fi
}

# Get SSH configuration
get_ssh_config() {
    print_header "SSH Configuration"

    GROUP_VARS_FILE="${ANSIBLE_CI_DIR}/group_vars/all.yml"

    if [[ -f "${GROUP_VARS_FILE}" ]]; then
        if command -v python3 &> /dev/null; then
            SSH_USER=$(python3 -c "import yaml; print(yaml.safe_load(open('${GROUP_VARS_FILE}')).get('ssh_username', 'ajasta'))" 2>/dev/null || echo "ajasta")
            SSH_KEY=$(python3 -c "import yaml; print(yaml.safe_load(open('${GROUP_VARS_FILE}')).get('ssh_private_key_file', ''))" 2>/dev/null || echo "")
        else
            SSH_USER=$(grep "^ssh_username:" "${GROUP_VARS_FILE}" | awk '{print $2}' | tr -d '"' || echo "ajasta")
            SSH_KEY=$(grep "^ssh_private_key_file:" "${GROUP_VARS_FILE}" | awk '{print $2}' | tr -d '"' || echo "")
        fi
    else
        SSH_USER="ajasta"
        SSH_KEY=""
    fi

    log_success "SSH user: ${SSH_USER}"

    # Auto-detect SSH key
    if [[ -z "${SSH_KEY}" ]] || [[ ! -f "${SSH_KEY}" ]]; then
        log_info "Auto-detecting SSH key..."
        for key in "$HOME/.ssh/id_rsa_k8s" "$HOME/.ssh/id_ed25519" "$HOME/.ssh/id_rsa"; do
            if [[ -f "${key}" ]]; then
                SSH_KEY="${key}"
                break
            fi
        done
    fi

    if [[ -n "${SSH_KEY}" ]] && [[ -f "${SSH_KEY}" ]]; then
        log_success "SSH key: ${SSH_KEY}"
    else
        log_warning "No SSH key found"
        SSH_KEY=""
    fi
}

# Generate inventory file
generate_inventory() {
    print_header "Generating Ansible Inventory from Yandex Cloud"

    # Create inventory
    cat > "${INVENTORY_FILE}" << EOF
# Ansible inventory generated from Yandex Cloud VMs
# Generated at: $(date -u +"%Y-%m-%d %H:%M:%S UTC")
# Source: Yandex Compute Cloud (actual running VMs)

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
    echo "Cluster topology (from Yandex Cloud):"
    echo "  Master:  ${MASTER_IP}"
    if [[ ${WORKER_INDEX} -gt 0 ]]; then
        echo "  Workers: ${WORKER_INDEX} node(s)"
        for i in $(seq 0 $((WORKER_INDEX - 1))); do
            echo "    worker-node-${i}: ${WORKER_LIST[$i]}"
        done
    else
        echo "  Workers: (none)"
    fi
}

# Test connectivity with direct SSH
test_connectivity() {
    print_header "Testing Connectivity"

    log_info "Testing SSH connectivity (direct)..."

    SUCCESS_COUNT=0
    FAILED_COUNT=0

    # SSH timeout and options
    SSH_TIMEOUT=10
    SSH_OPTS="-o StrictHostKeyChecking=no -o BatchMode=yes -o ConnectTimeout=${SSH_TIMEOUT} -o ServerAliveInterval=2 -o ServerAliveCountMax=1"

    # Test master
    echo -n "  Testing master-node (${MASTER_IP})... "
    MASTER_OUTPUT=""
    if [[ -n "${SSH_KEY}" ]]; then
        if MASTER_OUTPUT=$(ssh -i "${SSH_KEY}" ${SSH_OPTS} ${SSH_USER}@${MASTER_IP} "echo test" 2>&1); then
            if echo "${MASTER_OUTPUT}" | grep -q "test"; then
                echo -e "${GREEN}✓ REACHABLE${NC}"
                SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
            else
                echo -e "${RED}✗ UNREACHABLE${NC}"
                log_info "    Try: ssh -i ${SSH_KEY} ${SSH_USER}@${MASTER_IP}"
                FAILED_COUNT=$((FAILED_COUNT + 1))
            fi
        else
            echo -e "${RED}✗ UNREACHABLE${NC}"
            log_info "    Try: ssh -i ${SSH_KEY} ${SSH_USER}@${MASTER_IP}"
            FAILED_COUNT=$((FAILED_COUNT + 1))
        fi
    else
        if MASTER_OUTPUT=$(ssh ${SSH_OPTS} ${SSH_USER}@${MASTER_IP} "echo test" 2>&1); then
            if echo "${MASTER_OUTPUT}" | grep -q "test"; then
                echo -e "${GREEN}✓ REACHABLE${NC}"
                SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
            else
                echo -e "${RED}✗ UNREACHABLE${NC}"
                log_info "    Try: ssh ${SSH_USER}@${MASTER_IP}"
                FAILED_COUNT=$((FAILED_COUNT + 1))
            fi
        else
            echo -e "${RED}✗ UNREACHABLE${NC}"
            log_info "    Try: ssh ${SSH_USER}@${MASTER_IP}"
            FAILED_COUNT=$((FAILED_COUNT + 1))
        fi
    fi

    # Test workers
    for i in $(seq 0 $((${#WORKER_LIST[@]} - 1))); do
        WORKER_IP="${WORKER_LIST[$i]}"
        WORKER_NAME="worker-node-${i}"
        echo -n "  Testing ${WORKER_NAME} (${WORKER_IP})... "

        WORKER_OUTPUT=""
        if [[ -n "${SSH_KEY}" ]]; then
            if WORKER_OUTPUT=$(ssh -i "${SSH_KEY}" ${SSH_OPTS} ${SSH_USER}@${WORKER_IP} "echo test" 2>&1); then
                if echo "${WORKER_OUTPUT}" | grep -q "test"; then
                    echo -e "${GREEN}✓ REACHABLE${NC}"
                    SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
                else
                    echo -e "${RED}✗ UNREACHABLE${NC}"
                    log_info "    Try: ssh -i ${SSH_KEY} ${SSH_USER}@${WORKER_IP}"
                    FAILED_COUNT=$((FAILED_COUNT + 1))
                fi
            else
                echo -e "${RED}✗ UNREACHABLE${NC}"
                log_info "    Try: ssh -i ${SSH_KEY} ${SSH_USER}@${WORKER_IP}"
                FAILED_COUNT=$((FAILED_COUNT + 1))
            fi
        else
            if WORKER_OUTPUT=$(ssh ${SSH_OPTS} ${SSH_USER}@${WORKER_IP} "echo test" 2>&1); then
                if echo "${WORKER_OUTPUT}" | grep -q "test"; then
                    echo -e "${GREEN}✓ REACHABLE${NC}"
                    SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
                else
                    echo -e "${RED}✗ UNREACHABLE${NC}"
                    log_info "    Try: ssh ${SSH_USER}@${WORKER_IP}"
                    FAILED_COUNT=$((FAILED_COUNT + 1))
                fi
            else
                echo -e "${RED}✗ UNREACHABLE${NC}"
                log_info "    Try: ssh ${SSH_USER}@${WORKER_IP}"
                FAILED_COUNT=$((FAILED_COUNT + 1))
            fi
        fi
    done

    echo ""
    echo "Connectivity summary:"
    echo "  ✓ Reachable: ${SUCCESS_COUNT}"
    if [[ ${FAILED_COUNT} -gt 0 ]]; then
        echo "  ✗ Unreachable: ${FAILED_COUNT}"
        echo ""
        log_info "Common issues:"
        echo "  1. VMs still starting up (wait 2-3 minutes and retry)"
        echo "  2. Security groups not allowing SSH (port 22)"
        echo "  3. SSH key permissions: chmod 600 ${SSH_KEY}"
        echo "  4. Wrong SSH user or key"
        echo ""
        log_info "Test SSH manually:"
        if [[ -n "${SSH_KEY}" ]]; then
            echo "  ssh -i ${SSH_KEY} -v ${SSH_USER}@${MASTER_IP}"
        else
            echo "  ssh -v ${SSH_USER}@${MASTER_IP}"
        fi
        echo ""
        log_info "Inventory was generated successfully regardless."
        log_info "You can test connectivity later with:"
        echo "  cd ${ANSIBLE_CI_DIR}"
        echo "  ansible k8s -i inventory.ini -m ping"
    else
        log_success "All nodes reachable!"
    fi

    echo ""

    # Always return success (inventory is still valid)
    return 0
}

# Show warning
show_warning() {
    print_header "Important Notice"

    log_warning "Inventory generated from Yandex Cloud (not Terraform outputs)"
    echo ""
    echo "This inventory uses actual running VMs, not Terraform state."
    echo "This means:"
    echo "  1. Terraform state may be out of sync with actual infrastructure"
    echo "  2. Consider running terraform:apply to update state"
    echo "  3. Or update Terraform outputs manually:"
    echo "     yc compute instance list > terraform/vms.json"
    echo ""
    echo "For now, you can use this inventory to manage your cluster."
}

# Main
main() {
    clear

    print_header "Generate Ansible Inventory from Yandex Cloud"
    echo "This script generates inventory directly from Yandex Cloud VMs"
    echo "Use this when Terraform outputs are stale or unavailable."
    echo ""

    check_prerequisites
    get_vm_ips
    get_ssh_config
    generate_inventory
    show_warning

    test_connectivity

    echo ""
    log_success "Inventory generated successfully!"
    echo ""
    echo "Next steps:"
    echo "  1. Review inventory: cat ${INVENTORY_FILE}"
    echo "  2. Test connectivity:"
    echo "     cd ${ANSIBLE_CI_DIR}"
    echo "     ansible k8s -i inventory.ini -m ping"
    echo ""
    echo "  3. Run playbooks:"
    echo "     ansible-playbook -i inventory.ini <playbook.yml>"
    echo ""

    exit 0
}

main "$@"
