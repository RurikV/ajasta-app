#!/usr/bin/env bash
# Check which SSH key is configured in GitLab CI/CD variables
# This helps identify the correct private key to use for Ansible

set -euo pipefail

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

# Check for GITLAB_PAT
GITLAB_TOKEN="${GITLAB_PAT:-}"
if [[ -z "${GITLAB_TOKEN}" ]] || [[ "${GITLAB_TOKEN}" == "$" ]]; then
    log_error "GITLAB_PAT environment variable not set"
    echo ""
    echo "Please set your GitLab Personal Access Token:"
    echo "  export GITLAB_PAT=\"glpat-xxxxxxxxxxxxxxxxxxxx\""
    echo ""
    exit 1
fi

# Get project ID
PROJECT_ID="${PROJECT_ID:-1305}"  # Default to your project
GITLAB_HOST="otusteam.gitlab.yandexcloud.net"
GITLAB_API_URL="https://${GITLAB_HOST}/api/v4"

print_header "Checking GitLab CI/CD Variables for SSH Key Configuration"

log_info "Fetching CI/CD variables from GitLab..."

# Fetch variables (this will only work if you have maintainer access)
VARS_RESPONSE=$(curl -s --header "PRIVATE-TOKEN: ${GITLAB_TOKEN}" \
    "${GITLAB_API_URL}/projects/${PROJECT_ID}/variables" 2>/dev/null || echo "")

if [[ -z "${VARS_RESPONSE}" ]] || [[ "${VARS_RESPONSE}" == "[]" ]]; then
    log_warning "Could not fetch CI/CD variables (need maintainer access)"
    echo ""
    echo "Alternative: Check GitLab UI manually:"
    echo "  1. Go to: https://${GITLAB_HOST}/Vladimir.Rurik/ajasta-app/-/settings/ci_cd"
    echo "  2. Expand 'Variables' section"
    echo "  3. Look for: TF_VAR_ssh_public_key or TF_VAR_ssh_public_key_file"
    echo ""
else
    # Check for SSH-related variables
    echo ""
    log_info "SSH-related GitLab CI/CD variables:"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

    echo "${VARS_RESPONSE}" | jq -r '.[] | select(.key | contains("ssh") or contains("SSH")) | "\(.key): \(.value | if . == "" then "(empty)" else "(set - length: \(.value | length))" end)"' 2>/dev/null || echo "  No SSH variables found"
fi

echo ""
log_info "Available SSH keys on your system:"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

for key in ~/.ssh/id_rsa ~/.ssh/id_ed25519 ~/.ssh/id_rsa_k8s ~/.ssh/id_ecdsa ~/.ssh/id_ecdsa_k8s; do
    if [[ -f "${key}" ]]; then
        echo "  📁 ${key}"
        # Try to get the public key fingerprint
        if [[ -f "${key}.pub" ]]; then
            fingerprint=$(ssh-keygen -lf "${key}.pub" 2>/dev/null | awk '{print $2}')
            echo "     Fingerprint: ${fingerprint}"
        fi
    fi
done

echo ""
print_header "Instructions"

log_info "The VMs were created with an SSH public key from GitLab CI/CD."
echo ""
echo "To connect, you need the matching PRIVATE key. Here are your options:"
echo ""
echo "1. 📋 CHECK GITLAB UI:"
echo "   Go to: https://${GITLAB_HOST}/Vladimir.Rurik/ajasta-app/-/settings/ci_cd"
echo "   Look for variables:"
echo "     - TF_VAR_ssh_public_key (contains actual public key)"
echo "     - TF_VAR_ssh_public_key_file (contains path to public key file)"
echo ""
echo "2. 🔍 FIND MATCHING PRIVATE KEY:"
echo "   If you find the public key, compare it with your local public keys:"
echo ""

for key in ~/.ssh/id_rsa ~/.ssh/id_rsa_k8s ~/.ssh/id_ed25519; do
    if [[ -f "${key}" ]]; then
        pub_key="${key}.pub"
        if [[ -f "${pub_key}" ]]; then
            echo "   $(basename ${key}):"
            ssh-keygen -y -f "${key}" 2>/dev/null | head -c 80
            echo "..."
            echo ""
        fi
    fi
done

echo "3. ⚙️  CONFIGURE ANSIBLE:"
echo ""
echo "   Option A: Update group_vars/all.yml"
echo "   Edit ansible-ci/group_vars/all.yml:"
echo '     ssh_private_key_file: "/path/to/your/private/key"'
echo ""
echo "   Option B: Set environment variable"
echo '   export ANSIBLE_PRIVATE_KEY_FILE="/path/to/your/private/key"'
echo ""
echo "4. 🔄 REGENERATE INVENTORY:"
echo "   cd ansible-ci"
echo "   ./scripts/generate-inventory-from-terraform.sh"
echo ""
