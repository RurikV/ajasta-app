#!/bin/bash
# Kubernetes Cluster Setup Script
# This script runs all playbooks in the correct order to set up a complete Kubernetes cluster
#
# Usage:
#   ./k8s-cluster-setup.sh [start_step]
#
# Examples:
#   ./k8s-cluster-setup.sh       # Start from step 1
#   ./k8s-cluster-setup.sh 3     # Start from step 3 (resume)
#
# Prerequisites:
#   - Ansible installed
#   - SSH access to k8s-master and k8s-worker nodes configured
#   - SSH key: ~/.ssh/id_rsa_k8s
#   - All VMs already running

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Default starting step
START_STEP=${1:-1}

# Function to print colored output
print_step() {
    local step_num=$1
    local step_name=$2

    # If step_name is empty, this is the final verification step (no number)
    if [ -z "$step_name" ]; then
        echo -e "${BLUE}================================================================${NC}"
        echo -e "${GREEN}STEP: $step_num${NC}"
        echo -e "${BLUE}================================================================${NC}"
        return
    fi

    if [ "$step_num" -lt "$START_STEP" ]; then
        echo -e "${CYAN}================================================================${NC}"
        echo -e "${CYAN}SKIPPED: Step $step_num - $step_name${NC}"
        echo -e "${CYAN}================================================================${NC}"
    else
        echo -e "${BLUE}================================================================${NC}"
        echo -e "${GREEN}STEP $step_num: $step_name${NC}"
        echo -e "${BLUE}================================================================${NC}"
    fi
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_skip() {
    echo -e "${CYAN}⊘ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ ERROR: $1${NC}"
    exit 1
}

# Function to check if we should run this step
should_run_step() {
    local step_num=$1
    [ "$step_num" -ge "$START_STEP" ]
}

# Change to script directory
cd "$(dirname "$0")"

# Check if Ansible is installed
if ! command -v ansible &> /dev/null; then
    print_error "Ansible is not installed. Please install Ansible first."
fi

# Check if inventory file exists
if [ ! -f inventory.ini ]; then
    print_error "Inventory file not found at inventory.ini. Please create it first."
fi

# Print banner
echo -e "${BLUE}"
echo "╔════════════════════════════════════════════════════════════════╗"
echo "║   Kubernetes Cluster - Complete Setup Script              ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo -e "${NC}"
echo ""
if [ "$START_STEP" -eq 1 ]; then
    echo "This script will set up a complete Kubernetes cluster on:"
    echo "  - master-node (control plane)"
    echo "  - worker-node-0"
    echo "  - worker-node-1"
    echo "  - worker-node-2"
    echo ""
    echo "Steps:"
    echo "  1. Prepare nodes (disable firewall, configure sysctl)"
    echo "  2. Install containerd and nerdctl (container runtime)"
    echo "  3. Initialize Kubernetes control plane"
    echo "  4. Join worker nodes to cluster"
    echo "  5. Install Cilium CNI (network plugin)"
    echo "  6. Install Helm package manager"
    echo ""
    echo "Optional components (prompted after main setup):"
    echo "  - Kubernetes Dashboard"
    echo "  - Ingress NGINX Controller"
    echo "  - Longhorn Storage"
    echo "  - Test applications"
    echo ""
    echo "Estimated time: 10-15 minutes"
else
    echo -e "${YELLOW}RESUME MODE: Starting from step ${START_STEP}${NC}"
    echo ""
    echo "Steps before ${START_STEP} will be skipped."
    echo "To run from the beginning, use: ./k8s-cluster-setup.sh"
fi
echo ""

# Ask for confirmation (skip if not starting from step 1)
if [ "$START_STEP" -eq 1 ]; then
    read -p "Do you want to proceed? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Setup cancelled."
        exit 0
    fi
else
    echo -e "${YELLOW}Resuming from step ${START_STEP}...${NC}"
    sleep 2
fi

echo ""
echo "Starting setup..."
echo ""

# =================================================================
# STEP 1: Prepare Nodes
# =================================================================
print_step 1 "Preparing nodes for Kubernetes"

if should_run_step 1; then
    if ansible-playbook -i inventory.ini 01-prepare-nodes.yml; then
        print_success "Nodes prepared successfully"
    else
        print_error "Failed to prepare nodes"
    fi
else
    print_skip "Nodes already prepared"
fi

# =================================================================
# STEP 2: Install Container Runtime
# =================================================================
print_step 2 "Installing containerd and nerdctl"

if should_run_step 2; then
    if ansible-playbook -i inventory.ini 5-install-containerd-nerdctl.yml; then
        print_success "Container runtime installed"
    else
        print_error "Failed to install container runtime"
    fi
else
    print_skip "Container runtime already installed"
fi

# =================================================================
# STEP 3: Initialize Control Plane
# =================================================================
print_step 3 "Initializing Kubernetes control plane"

if should_run_step 3; then
    if ansible-playbook -i inventory.ini 02-init-master.yml; then
        print_success "Control plane initialized"
    else
        print_error "Failed to initialize control plane"
    fi
else
    print_skip "Control plane already initialized"
fi

# =================================================================
# STEP 4: Join Worker Nodes
# =================================================================
print_step 4 "Joining worker nodes to cluster"

if should_run_step 4; then
    if ansible-playbook -i inventory.ini 04-join-workers.yml; then
        print_success "Worker nodes joined"
    else
        print_error "Failed to join worker nodes"
    fi
else
    print_skip "Worker nodes already joined"
fi

# =================================================================
# STEP 5: Install Cilium CNI
# =================================================================
print_step 5 "Installing Cilium CNI"

if should_run_step 5; then
    if ansible-playbook -i inventory.ini 03-install-cilium.yml; then
        print_success "Cilium CNI installed"
    else
        print_error "Failed to install Cilium"
    fi
else
    print_skip "Cilium CNI already installed"
fi

# =================================================================
# STEP 6: Install Helm
# =================================================================
print_step 6 "Installing Helm package manager"

if should_run_step 6; then
    if ansible-playbook -i inventory.ini 09-install-helm-binary.yml; then
        print_success "Helm installed"
    else
        print_error "Failed to install Helm"
    fi
else
    print_skip "Helm already installed"
fi

# =================================================================
# OPTIONAL: Additional Components
# =================================================================
# Ask if user wants to install additional components
if [ $START_STEP -le 6 ]; then
    echo ""
    read -p "Do you want to install additional components (Dashboard, Ingress, Longhorn)? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        # Kubernetes Dashboard
        print_step 7 "Deploying Kubernetes Dashboard"
        if ansible-playbook -i inventory.ini 10-deploy-kubernetes-dashboard.yml; then
            print_success "Kubernetes Dashboard deployed"
        else
            print_error "Failed to deploy Dashboard"
        fi

        # Ingress NGINX
        print_step 8 "Deploying Ingress NGINX Controller"
        if ansible-playbook -i inventory.ini 13-deploy-ingress-nginx-controller.yml; then
            print_success "Ingress NGINX Controller deployed"
        else
            print_error "Failed to deploy Ingress"
        fi

        # Longhorn Storage
        print_step 9 "Deploying Longhorn Storage"
        if ansible-playbook -i inventory.ini 14-deploy-longhorn-storage.yml; then
            print_success "Longhorn Storage deployed"
        else
            print_error "Failed to deploy Longhorn"
        fi
    fi
fi

# If resuming from step 7-9, run those steps
if [ $START_STEP -ge 7 ] && [ $START_STEP -le 9 ]; then
    if should_run_step 7; then
        print_step 7 "Deploying Kubernetes Dashboard"
        if ansible-playbook -i inventory.ini 10-deploy-kubernetes-dashboard.yml; then
            print_success "Kubernetes Dashboard deployed"
        else
            print_error "Failed to deploy Dashboard"
        fi
    fi

    if should_run_step 8; then
        print_step 8 "Deploying Ingress NGINX Controller"
        if ansible-playbook -i inventory.ini 13-deploy-ingress-nginx-controller.yml; then
            print_success "Ingress NGINX Controller deployed"
        else
            print_error "Failed to deploy Ingress"
        fi
    fi

    if should_run_step 9; then
        print_step 9 "Deploying Longhorn Storage"
        if ansible-playbook -i inventory.ini 14-deploy-longhorn-storage.yml; then
            print_success "Longhorn Storage deployed"
        else
            print_error "Failed to deploy Longhorn"
        fi
    fi
fi

# =================================================================
# FINAL VERIFICATION
# =================================================================
echo ""
print_step "Verifying cluster installation"

# Get cluster info via Ansible
echo ""
echo "=== Cluster Nodes ==="
ansible -i inventory.ini k8s_master -m shell -a 'sudo kubectl get nodes -o wide' 2>/dev/null || \
    echo "Skipping node verification"

echo ""
echo "=== Cluster Pods ==="
ansible -i inventory.ini k8s_master -m shell -a 'sudo kubectl get pods -A' 2>/dev/null || \
    echo "Skipping pod verification"

# =================================================================
# SUMMARY
# =================================================================
echo ""
echo -e "${GREEN}╔════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║   KUBERNETES CLUSTER SETUP COMPLETE!                             ║${NC}"
echo -e "${GREEN}╚════════════════════════════════════════════════════════════════╝${NC}"
echo ""
echo "Cluster Information:"
echo "  - Master: master-node (89.169.182.221)"
echo "  - Workers: worker-node-0, worker-node-1, worker-node-2"
echo "  - CNI: Cilium (with Hubble observability)"
echo "  - K8s Version: 1.34.3"
echo ""
echo "Access Information:"
echo ""
echo "1. SSH into master node:"
echo "   ssh -i ~/.ssh/id_rsa_k8s ajasta@89.169.182.221"
echo ""
echo "2. Use kubectl on master:"
echo "   sudo kubectl get nodes"
echo "   sudo kubectl get pods -A"
echo ""
echo "3. Update local kubeconfig:"
echo "   ./scripts/update-kubeconfig.sh  # from ansible-ci directory"
echo "   or"
echo "   kubectl config set-cluster k8s-cluster --server=https://89.169.182.221:6443"
echo ""
echo "4. Access cluster from local machine:"
echo "   export KUBECONFIG=~/.kube/config"
echo "   kubectl get nodes"
echo ""
echo "5. Cilium Hubble UI (Network Observability):"
echo "   kubectl port-forward -n kube-system svc/hubble-ui 12000:80"
echo "   Open: http://localhost:12000"
echo ""
echo "6. Kubernetes Dashboard:"
echo "   kubectl port-forward -n kubernetes-dashboard svc/kubernetes-dashboard-kong-proxy 8443:443"
echo "   Open: https://localhost:8443"
echo "   Get token: kubectl -n kubernetes-dashboard create token dashboard-admin"
echo ""
echo "Quick Commands:"
echo "  - Check cluster: ansible -i inventory.ini k8s_master -m shell -a 'sudo kubectl get nodes'"
echo "  - Check pods: ansible -i inventory.ini k8s_master -m shell -a 'sudo kubectl get pods -A'"
echo "  - Destroy cluster: ansible-playbook -i inventory.ini 00-destroy-k8s-cluster.yml"
echo ""
echo "Optional Next Steps:"
echo "  - Deploy test application: ansible-playbook -i inventory.ini 06-deploy-test-app.yml"
echo "  - Deploy CloudNativePG: ansible-playbook -i inventory.ini 19-deploy-cloudnativepg.yml"
echo "  - Deploy nginx chart: ansible-playbook -i inventory.ini 17-deploy-nginx-chart.yml"
echo ""
echo "Documentation:"
echo "  - See README.md for detailed documentation"
echo "  - All playbooks are located in this directory"
echo ""
echo -e "${BLUE}================================================================${NC}"
echo -e "${GREEN}Setup completed successfully!${NC}"
echo -e "${BLUE}================================================================${NC}"
