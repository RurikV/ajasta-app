#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

# Default values
KUBERNETES_TARGET_VERSION="${KUBERNETES_TARGET_VERSION:-1.30.0}"
INVENTORY_FILE="${PROJECT_ROOT}/inventory.ini"
FORCE_UPGRADE="${FORCE_UPGRADE:-false}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Display usage
usage() {
    cat <<EOF
Usage: ${0} [OPTIONS]

Upgrade Kubernetes cluster to a new version.

OPTIONS:
    -v, --version VERSION       Target Kubernetes version (default: 1.30.0)
    -i, --inventory FILE        Inventory file (default: inventory.ini)
    -f, --force                 Skip confirmation prompts
    -h, --help                  Show this help message

EXAMPLES:
    # Upgrade to Kubernetes 1.30.0 (default)
    ${0}

    # Upgrade to Kubernetes 1.30.2
    ${0} --version 1.30.2

    # Upgrade without confirmation
    ${0} --version 1.30.0 --force

ENVIRONMENT VARIABLES:
    KUBERNETES_TARGET_VERSION    Target version to upgrade TO
EOF
}

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -v|--version)
            KUBERNETES_TARGET_VERSION="$2"
            shift 2
            ;;
        -i|--inventory)
            INVENTORY_FILE="$2"
            shift 2
            ;;
        -f|--force)
            FORCE_UPGRADE=true
            shift
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
Kubernetes Cluster Upgrade Script
==========================================
Target Version: ${KUBERNETES_TARGET_VERSION}
Inventory: ${INVENTORY_FILE}
==========================================
${NC}"

# Check if inventory file exists
if [ ! -f "${INVENTORY_FILE}" ]; then
    echo -e "${RED}Error: Inventory file not found: ${INVENTORY_FILE}${NC}"
    exit 1
fi

# Check if cluster is accessible
echo -e "${YELLOW}Checking cluster status...${NC}"
if ! kubectl get nodes &>/dev/null; then
    echo -e "${RED}Error: Cannot access cluster. Please check your kubeconfig.${NC}"
    echo ""
    echo "Try:"
    echo "  export KUBECONFIG=\$HOME/.kube/config"
    echo "  ./scripts/update-kubeconfig.sh"
    exit 1
fi

# Get current version
CURRENT_VERSION=$(kubectl version --short 2>/dev/null | grep "Server Version" | awk '{print $3}' | sed 's/^v//')

if [ -z "$CURRENT_VERSION" ]; then
    CURRENT_VERSION=$(kubectl version -o json 2>/dev/null | jq -r '.serverVersion.gitVersion' | sed 's/^v//')
fi

if [ -z "$CURRENT_VERSION" ]; then
    echo -e "${RED}Error: Cannot detect current Kubernetes version${NC}"
    exit 1
fi

echo -e "${GREEN}Current version: ${CURRENT_VERSION}${NC}"
echo -e "${GREEN}Target version:  ${KUBERNETES_TARGET_VERSION}${NC}"
echo ""

# Validate upgrade path
if ! python3 -c "import sys; from packaging import version; sys.exit(0 if version.Version('${KUBERNETES_TARGET_VERSION}') >= version.Version('${CURRENT_VERSION}') else 1)" 2>/dev/null; then
    if [ "$(echo "${KUBERNETES_TARGET_VERSION}" | cut -d. -f1-2)" -lt "$(echo "${CURRENT_VERSION}" | cut -d. -f1-2)" ]; then
        echo -e "${RED}Error: Target version must be greater than or equal to current version${NC}"
        echo -e "${RED}  Current: ${CURRENT_VERSION}${NC}"
        echo -e "${RED}  Target:  ${KUBERNETES_TARGET_VERSION}${NC}"
        exit 1
    fi
fi

# Check for version skew
CURRENT_MINOR=$(echo "${CURRENT_VERSION}" | cut -d. -f2)
TARGET_MINOR=$(echo "${KUBERNETES_TARGET_VERSION}" | cut -d. -f2)

VERSION_DIFF=$((TARGET_MINOR - CURRENT_MINOR))

if [ $VERSION_DIFF -gt 1 ]; then
    echo -e "${YELLOW}Warning: Skipping multiple minor versions (${CURRENT_VERSION} -> ${KUBERNETES_TARGET_VERSION})${NC}"
    echo ""
    echo "Kubernetes recommends upgrading one minor version at a time."
    echo ""
    echo "Suggested upgrade path:"
    NEXT_MINOR=$((CURRENT_MINOR + 1))
    echo "  1. ${CURRENT_VERSION} -> 1.${NEXT_MINOR}.x"
    echo "  2. 1.${NEXT_MINOR}.x -> ${KUBERNETES_TARGET_VERSION}"
    echo ""
    read -p "Continue anyway? (y/N) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo -e "${RED}Upgrade cancelled${NC}"
        exit 0
    fi
fi

# Display what will happen
echo ""
echo -e "${YELLOW}This upgrade will:${NC}"
echo "  1. Pre-upgrade health checks"
echo "  2. Upgrade master node (control plane)"
echo "  3. Upgrade worker nodes one by one"
echo "  4. Drain nodes during upgrade (downtime for workloads)"
echo "  5. Verify cluster health after upgrade"
echo ""

if [ "$FORCE_UPGRADE" = "false" ]; then
    read -p "Continue with upgrade? (y/N) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo -e "${RED}Upgrade cancelled${NC}"
        exit 0
    fi
fi

# Create backup timestamp
BACKUP_DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="${PROJECT_ROOT}/backups/${BACKUP_DATE}"
mkdir -p "${BACKUP_DIR}"

# Backup cluster state
echo ""
echo -e "${GREEN}Creating backup...${NC}"
kubectl get all -A --output=json > "${BACKUP_DIR}/cluster-backup.json" 2>/dev/null || true
kubectl get nodes -o yaml > "${BACKUP_DIR}/nodes-backup.yaml" 2>/dev/null || true
echo "Backup saved to: ${BACKUP_DIR}"

# Run Ansible playbook
echo ""
echo -e "${GREEN}Starting cluster upgrade...${NC}"
echo ""

cd "${PROJECT_ROOT}"

if ansible-playbook -i "${INVENTORY_FILE}" k8s-upgrade.yml \
    -e "kubernetes_target_version=${KUBERNETES_TARGET_VERSION}"; then

    echo ""
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}✅ Cluster upgrade complete!${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo ""
    echo "Upgraded from: ${CURRENT_VERSION}"
    echo "Upgraded to:   ${KUBERNETES_TARGET_VERSION}"
    echo ""
    echo "Next steps:"
    echo "  1. Verify all pods are running:"
    echo "     kubectl get pods -A"
    echo ""
    echo "  2. Check cluster health:"
    echo "     kubectl get cs"
    echo ""
    echo "  3. Test your applications"
    echo ""
    echo "Backup location: ${BACKUP_DIR}"
    echo ""

    # Show cluster status
    echo -e "${YELLOW}Cluster nodes:${NC}"
    kubectl get nodes
    echo ""

    exit 0
else
    echo ""
    echo -e "${RED}========================================${NC}"
    echo -e "${RED}❌ Cluster upgrade failed!${NC}"
    echo -e "${RED}========================================${NC}"
    echo ""
    echo "Please check the error messages above."
    echo ""
    echo "Cluster may be in partially upgraded state."
    echo "Backup available at: ${BACKUP_DIR}"
    echo ""
    echo "To rollback, see documentation:"
    echo "  ${PROJECT_ROOT}/K8S_INSTALLATION_UPGRADE_GUIDE.md"
    echo ""
    exit 1
fi
