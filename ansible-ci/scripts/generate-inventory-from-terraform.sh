#!/usr/bin/env bash
# Generate Ansible inventory from Terraform outputs
# This script reads terraform/outputs.json and creates ansible-ci/inventory.ini

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
NC='\033[0m' # No Color

# Check if outputs.json exists
if [[ ! -f "${OUTPUTS_FILE}" ]]; then
    echo -e "${RED}Error: Terraform outputs file not found: ${OUTPUTS_FILE}${NC}"
    echo -e "${YELLOW}Please run 'terraform apply' first to generate the outputs${NC}"
    exit 1
fi

# Check if jq is installed
if ! command -v jq &> /dev/null; then
    echo -e "${RED}Error: jq is required but not installed${NC}"
    echo "Install with: brew install jq (macOS) or apt-get install jq (Linux)"
    exit 1
fi

# Read Terraform outputs (handle both GitLab format with .value and direct format)
MASTER_IP=$(jq -r '.master_public_ip | if type == "object" and has("value") then .value elif type == "string" then . else . end' "${OUTPUTS_FILE}")
WORKER_IPS=$(jq -r '.worker_public_ips | if type == "object" and has("value") then .value elif type == "object" then . else . end' "${OUTPUTS_FILE}")

# Read SSH username from group_vars
GROUP_VARS_FILE="${ANSIBLE_CI_DIR}/group_vars/all.yml"
if [[ -f "${GROUP_VARS_FILE}" ]]; then
    SSH_USER=$(grep "^ssh_username:" "${GROUP_VARS_FILE}" | awk '{print $2}' | tr -d '"')
    SSH_KEY=$(grep "^ssh_private_key_file:" "${GROUP_VARS_FILE}" | awk '{print $2}' | tr -d '"')
else
    SSH_USER="ajasta"  # Default fallback
    SSH_KEY=""  # Default fallback
fi

# Auto-detect SSH key if not specified
if [[ -z "${SSH_KEY}" ]]; then
    # Try common SSH key locations
    for key in "$HOME/.ssh/id_rsa" "$HOME/.ssh/id_ed25519" "$HOME/.ssh/id_rsa_k8s"; do
        if [[ -f "${key}" ]]; then
            SSH_KEY="${key}"
            break
        fi
    done
fi

# Validate master IP
if [[ -z "${MASTER_IP}" ]] || [[ "${MASTER_IP}" == "null" ]]; then
    echo -e "${RED}Error: Could not extract master_public_ip from Terraform outputs${NC}"
    echo "Contents of outputs.json:"
    jq '.' "${OUTPUTS_FILE}"
    exit 1
fi

# Create inventory file
if [[ -n "${SSH_KEY}" ]]; then
    cat > "${INVENTORY_FILE}" << EOF
# Ansible inventory generated from Terraform outputs
# Generated at: $(date -u +"%Y-%m-%d %H:%M:%S UTC")
# DO NOT EDIT MANUALLY - Use generate-inventory-from-terraform.sh to regenerate

[k8s-master]
k8s-master ansible_host=${MASTER_IP} ansible_user=${SSH_USER} ansible_ssh_private_key_file=${SSH_KEY}

[k8s-workers]
EOF
else
    cat > "${INVENTORY_FILE}" << EOF
# Ansible inventory generated from Terraform outputs
# Generated at: $(date -u +"%Y-%m-%d %H:%M:%S UTC")
# DO NOT EDIT MANUALLY - Use generate-inventory-from-terraform.sh to regenerate

[k8s-master]
k8s-master ansible_host=${MASTER_IP} ansible_user=${SSH_USER}

[k8s-workers]
EOF
fi

# Add workers to inventory
WORKER_COUNT=0
if [[ -n "${WORKER_IPS}" ]] && [[ "${WORKER_IPS}" != "null" ]] && [[ "${WORKER_IPS}" != "{}" ]]; then
    # Parse worker IPs from JSON object
    for key in $(echo "${WORKER_IPS}" | jq -r 'keys[]'); do
        WORKER_IP=$(echo "${WORKER_IPS}" | jq -r ".[\"${key}\"]")
        if [[ -n "${WORKER_IP}" ]] && [[ "${WORKER_IP}" != "null" ]]; then
            if [[ -n "${SSH_KEY}" ]]; then
                echo "k8s-worker-${WORKER_COUNT} ansible_host=${WORKER_IP} ansible_user=${SSH_USER} ansible_ssh_private_key_file=${SSH_KEY}" >> "${INVENTORY_FILE}"
            else
                echo "k8s-worker-${WORKER_COUNT} ansible_host=${WORKER_IP} ansible_user=${SSH_USER}" >> "${INVENTORY_FILE}"
            fi
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

echo -e "${GREEN}✅ Ansible inventory generated successfully!${NC}"
echo -e "${GREEN}   Inventory file: ${INVENTORY_FILE}${NC}"
echo ""
echo "Cluster nodes:"
echo "  Master:  ${MASTER_IP}"
if [[ ${WORKER_COUNT} -gt 0 ]]; then
    echo "  Workers: ${WORKER_COUNT} node(s)"
    grep "k8s-worker" "${INVENTORY_FILE}" | sed 's/^/    /'
else
    echo "  Workers: (none - master-only cluster)"
fi
echo ""
echo "Test connectivity with:"
echo "  ansible k8s-master -i ${INVENTORY_FILE} -m ping"
echo "  ansible k8s -i ${INVENTORY_FILE} -m ping"
