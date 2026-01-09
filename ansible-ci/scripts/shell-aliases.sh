#!/bin/bash
# Shell Aliases for Ajasta Kubernetes Management
# Source this file in your ~/.bashrc or ~/.zshrc:
#   source ~/IdeaProjects/petrelevich/ajasta-app/ansible-ci/scripts/shell-aliases.sh

# Project directory
export AJASTA_PROJECT="/Users/rurik/IdeaProjects/petrelevich/ajasta-app"
export AJASTA_ANSIBLE="$AJASTA_PROJECT/ansible-ci"
export AJASTA_TERRAFORM="$AJASTA_PROJECT/terraform"

# Aliases
alias aj-k8s-update='cd $AJASTA_ANSIBLE && ./scripts/update-kubeconfig.sh && cd -'
alias aj-k8s-ip='cd $AJASTA_ANSIBLE && ./scripts/update-kubeconfig.sh --print-ip-only'
alias aj-k8s-post-terraform='cd $AJASTA_ANSIBLE && ./scripts/post-terraform-setup.sh'
alias aj-k8s-status='kubectl get nodes && kubectl top nodes'
alias aj-k8s-pods='kubectl get pods -A'
alias aj-k8s-ansible='cd $AJASTA_ANSIBLE'
alias aj-tf-terraform='cd $AJASTA_TERRAFORM'

# Functions
aj-k8s-update-and-test() {
  echo "🔄 Updating kubeconfig..."
  cd "$AJASTA_ANSIBLE" || return 1
  ./scripts/update-kubeconfig.sh
  echo ""
  echo "🧪 Testing cluster connectivity..."
  kubectl get nodes
  cd - || return 1
}

aj-k8s-full-update() {
  echo "🚀 Full Kubernetes environment update..."
  echo ""
  echo "Step 1: Generating inventory..."
  cd "$AJASTA_ANSIBLE" || return 1
  ./scripts/generate-inventory-from-terraform.sh
  echo ""
  echo "Step 2: Updating kubeconfig..."
  ./scripts/update-kubeconfig.sh
  echo ""
  echo "Step 3: Testing cluster..."
  kubectl get nodes
  echo ""
  echo "✅ Update complete!"
  cd - || return 1
}

# Show current cluster info
aj-k8s-info() {
  echo "=== Ajasta Kubernetes Cluster Info ==="
  echo ""
  echo "Master IP:"
  cd "$AJASTA_ANSIBLE" || return 1
  ./scripts/update-kubeconfig.sh --print-ip-only
  cd - || return 1
  echo ""
  echo "Kubeconfig:"
  echo "  Path: ${KUBECONFIG:-$HOME/.kube/config}"
  echo ""
  echo "Cluster Status:"
  kubectl cluster-info 2>/dev/null || echo "  Not connected or bootstrap not complete"
  echo ""
  echo "Nodes:"
  kubectl get nodes 2>/dev/null || echo "  Not connected"
  echo ""
}

export -f aj-k8s-update-and-test
export -f aj-k8s-full-update
export -f aj-k8s-info

# Helpful message when sourced
echo "✅ Ajasta Kubernetes aliases loaded!"
echo ""
echo "Quick commands:"
echo "  aj-k8s-info           - Show cluster info"
echo "  aj-k8s-update         - Update kubeconfig with latest Terraform IPs"
echo "  aj-k8s-full-update    - Full update (inventory + kubeconfig + test)"
echo "  aj-k8s-status         - Show node status"
echo "  aj-k8s-pods           - Show all pods"
echo "  aj-k8s-ansible        - Go to Ansible directory"
echo "  aj-tf-terraform       - Go to Terraform directory"
echo ""
echo "For more info, see: ansible-ci/KUBECONFIG_UPDATE_GUIDE.md"
echo ""
