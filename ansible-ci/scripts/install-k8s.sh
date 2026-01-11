#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

# Default values
KUBERNETES_VERSION="${KUBERNETES_VERSION:-1.29.15}"
INVENTORY_FILE="${PROJECT_ROOT}/inventory.ini"
ANSIBLE_PLAYBOOK="ansible-playbook"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Display usage
usage() {
    cat <<EOF
Usage: ${0} [OPTIONS]

Install Kubernetes cluster on existing VMs.

OPTIONS:
    -v, --version VERSION       Kubernetes version to install (default: 1.29.15)
    -i, --inventory FILE        Inventory file (default: inventory.ini)
    -h, --help                  Show this help message

EXAMPLES:
    # Install Kubernetes 1.29.15 (default)
    ${0}

    # Install Kubernetes 1.30.0
    ${0} --version 1.30.0

    # Install with custom inventory
    ${0} --inventory /path/to/inventory.ini

ENVIRONMENT VARIABLES:
    KUBERNETES_VERSION          Kubernetes version to install
EOF
}

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -v|--version)
            KUBERNETES_VERSION="$2"
            shift 2
            ;;
        -i|--inventory)
            INVENTORY_FILE="$2"
            shift 2
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            echo -e "${RED}Error: Unknown option: $1${NC}"
            usage
            exit 1
            ;;
    esac
done

# Display banner
echo -e "${GREEN}"
cat <<EOF
==========================================
Kubernetes Installation Script
==========================================
Version: ${KUBERNETES_VERSION}
Inventory: ${INVENTORY_FILE}
==========================================
${NC}"

# Check if inventory file exists
if [ ! -f "${INVENTORY_FILE}" ]; then
    echo -e "${RED}Error: Inventory file not found: ${INVENTORY_FILE}${NC}"
    echo ""
    echo "Please generate inventory first:"
    echo "  cd ${PROJECT_ROOT}"
    echo "  ./scripts/generate-inventory-from-terraform.sh"
    exit 1
fi

# Check if terraform outputs exist
if [ ! -f "${PROJECT_ROOT}/../terraform/outputs.json" ]; then
    echo -e "${YELLOW}Warning: Terraform outputs not found at ${PROJECT_ROOT}/../terraform/outputs.json${NC}"
    echo ""
    echo "If VMs are not yet created, run Terraform first:"
    echo "  cd ${PROJECT_ROOT}/../terraform"
    echo "  terraform apply"
    echo ""
    read -p "Continue anyway? (y/N) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

# Pre-flight checks
echo -e "${GREEN}Running pre-flight checks...${NC}"

# Check if ansible-playbook is available
if ! command -v ansible-playbook &> /dev/null; then
    echo -e "${RED}Error: ansible-playbook not found. Please install Ansible first.${NC}"
    exit 1
fi

# Check if inventory has required groups
if ! grep -q "\[k8s-master\]" "${INVENTORY_FILE}"; then
    echo -e "${RED}Error: [k8s-master] group not found in inventory${NC}"
    exit 1
fi

if ! grep -q "\[k8s-workers\]" "${INVENTORY_FILE}"; then
    echo -e "${RED}Error: [k8s-workers] group not found in inventory${NC}"
    exit 1
fi

# Display what will be done
echo ""
echo -e "${YELLOW}This will:${NC}"
echo "  1. Install Kubernetes ${KUBERNETES_VERSION} on all nodes"
echo "  2. Initialize the master node"
echo "  3. Install CNI plugin (Flannel)"
echo "  4. Join worker nodes to the cluster"
echo ""
read -p "Continue? (y/N) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${RED}Installation cancelled${NC}"
    exit 0
fi

# Run Ansible playbook
echo ""
echo -e "${GREEN}Starting Kubernetes installation...${NC}"
echo ""

cd "${PROJECT_ROOT}"

if ansible-playbook -i "${INVENTORY_FILE}" k8s-install.yml \
    -e "kubernetes_version=${KUBERNETES_VERSION}"; then

    echo ""
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}✅ Kubernetes installation complete!${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo ""
    echo "Next steps:"
    echo "  1. Update local kubeconfig:"
    echo "     ./scripts/update-kubeconfig.sh"
    echo ""
    echo "  2. Verify cluster:"
    echo "     export KUBECONFIG=\$HOME/.kube/config"
    echo "     kubectl get nodes"
    echo "     kubectl get pods -A"
    echo ""
    echo "  3. Deploy applications:"
    echo "     ansible-playbook -i inventory.ini deploy-apps.yml"
    echo ""

    # Offer to update kubeconfig
    read -p "Update kubeconfig now? (y/N) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        ./scripts/update-kubeconfig.sh
    fi

    exit 0
else
    echo ""
    echo -e "${RED}========================================${NC}"
    echo -e "${RED}❌ Kubernetes installation failed!${NC}"
    echo -e "${RED}========================================${NC}"
    echo ""
    echo "Please check the error messages above and:"
    echo "  1. Verify VMs are running"
    echo "  2. Check SSH connectivity"
    echo "  3. Review Ansible logs"
    echo ""
    exit 1
fi
