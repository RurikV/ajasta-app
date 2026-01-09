#!/bin/bash
# Post-Terraform Setup Script
# This script should be run after Terraform apply to:
# 1. Generate Ansible inventory from Terraform outputs
# 2. Update local kubeconfig with new master IP

set -e

echo "=== Post-Terraform Setup ==="
echo ""

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ANSIBLE_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

# Step 1: Generate Ansible inventory
echo "📝 Step 1: Generating Ansible inventory..."
"${ANSIBLE_DIR}/scripts/generate-inventory-from-terraform.sh"

echo ""
echo "📝 Step 2: Updating kubeconfig..."
"${ANSIBLE_DIR}/scripts/update-kubeconfig.sh"

echo ""
echo "✅ Setup complete!"
echo ""
echo "Next steps:"
echo "  1. Bootstrap cluster: cd ${ANSIBLE_DIR} && ansible-playbook -i inventory.ini k8s-bootstrap.yml"
echo "  2. After bootstrap, update kubeconfig: ${ANSIBLE_DIR}/scripts/update-kubeconfig.sh"
echo ""
