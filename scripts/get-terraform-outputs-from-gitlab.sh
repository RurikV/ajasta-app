#!/usr/bin/env bash
# Fetch Terraform outputs from GitLab HTTP backend
# This script retrieves outputs.json from GitLab CI/CD Terraform state
# Supports both gitlab.com and self-hosted GitLab instances

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
TERRAFORM_DIR="${PROJECT_ROOT}/terraform"
OUTPUTS_FILE="${TERRAFORM_DIR}/outputs.json"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
ENVIRONMENT="${1:-production}"  # production or staging

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

    # Check for curl
    if ! command -v curl &> /dev/null; then
        log_error "curl is required but not installed"
        exit 1
    fi
    log_success "curl is installed"

    # Get GITLAB_PAT
    GITLAB_TOKEN="${GITLAB_PAT:-}"
    if [[ -z "${GITLAB_TOKEN}" ]] || [[ "${GITLAB_TOKEN}" == "\$" ]]; then
        log_error "GITLAB_PAT environment variable not set"
        echo ""
        echo "Please set your GitLab Personal Access Token:"
        echo "  export GITLAB_PAT=\"glpat-xxxxxxxxxxxxxxxxxxxx\""
        echo ""
        echo "Create token at: https://otusteam.gitlab.yandexcloud.net/-/user_settings/personal_access_tokens"
        echo "Required scope: api"
        echo ""
        echo "For zsh, make sure to use double quotes:"
        echo '  export GITLAB_PAT="glpat-xxxxxxxxxxxxxxxxxxxx"'
        exit 1
    fi
    log_success "GitLab token is configured"
}

# Detect GitLab instance and project info
detect_gitlab_info() {
    print_header "Detecting GitLab Instance"

    # Get git remote
    if ! git remote get-url origin &> /dev/null; then
        log_error "Not in a git repository or no origin remote found"
        exit 1
    fi

    GIT_REMOTE=$(git remote get-url origin)
    log_info "Git remote: ${GIT_REMOTE}"

    # Detect GitLab host and extract project path
    if [[ "${GIT_REMOTE}" =~ git@([^:]+):(.+)\/(.+)\.git ]]; then
        # SSH format: git@gitlab.com:group/project.git
        GITLAB_HOST="${BASH_REMATCH[1]}"
        GITLAB_PATH="${BASH_REMATCH[2]}/${BASH_REMATCH[3]}"
    elif [[ "${GIT_REMOTE}" =~ https?://([^/]+)\/(.+)\/(.+)\.git ]]; then
        # HTTPS format: https://gitlab.com/group/project.git
        GITLAB_HOST="${BASH_REMATCH[1]}"
        GITLAB_PATH="${BASH_REMATCH[2]}/${BASH_REMATCH[3]}"
    else
        log_error "Could not parse Git remote URL"
        echo "Git remote: ${GIT_REMOTE}"
        exit 1
    fi

    # Construct GitLab API URL
    GITLAB_API_URL="https://${GITLAB_HOST}/api/v4"
    log_success "GitLab instance: ${GITLAB_API_URL}"
    log_info "Project path: ${GITLAB_PATH}"

    # Initialize PROJECT_ID variable
    LOCAL_PROJECT_ID="${PROJECT_ID:-}"

    # Get project ID (allow manual setting or auto-detection)
    if [[ -n "${LOCAL_PROJECT_ID}" ]]; then
        log_success "Using manually set PROJECT_ID: ${LOCAL_PROJECT_ID}"
    else
        log_info "Attempting to resolve project ID automatically..."
        ENCODED_PATH=$(echo "${GITLAB_PATH}" | jq -sRr @uri)

        PROJECT_SEARCH=$(curl -s --header "PRIVATE-TOKEN: ${GITLAB_TOKEN}" \
            "${GITLAB_API_URL}/projects?search=${ENCODED_PATH}")

        LOCAL_PROJECT_ID=$(echo "${PROJECT_SEARCH}" | jq -r '.[0].id // empty')

        if [[ -z "${LOCAL_PROJECT_ID}" ]] || [[ "${LOCAL_PROJECT_ID}" == "null" ]]; then
            log_warning "Could not auto-detect project ID (common with self-hosted GitLab)"
            echo ""
            echo "Please find your project ID manually:"
            echo "  1. Go to: https://${GITLAB_HOST}/${GITLAB_PATH}"
            echo "  2. Look at the URL when you click on 'Project Overview' or 'Infrastructure'"
            echo "  3. The URL will show: https://${GITLAB_HOST}/(<PROJECT_ID>)/..."
            echo ""
            read -p "Enter your Project ID (or press Ctrl+C to cancel): " MANUAL_PROJECT_ID
            LOCAL_PROJECT_ID="${MANUAL_PROJECT_ID}"

            if [[ -z "${LOCAL_PROJECT_ID}" ]]; then
                log_error "Project ID not provided"
                exit 1
            fi
        fi
    fi

    log_success "Project ID: ${LOCAL_PROJECT_ID}"

    # Set PROJECT_ID for use in fetch_terraform_state function
    PROJECT_ID="${LOCAL_PROJECT_ID}"
}

# Fetch Terraform state from GitLab
fetch_terraform_state() {
    print_header "Fetching Terraform State from GitLab"

    # Remove old outputs file to ensure we don't use stale data
    if [[ -f "${OUTPUTS_FILE}" ]]; then
        log_info "Removing old outputs file: ${OUTPUTS_FILE}"
        rm -f "${OUTPUTS_FILE}"
    fi

    local state_url="${GITLAB_API_URL}/projects/${PROJECT_ID}/terraform/state/${ENVIRONMENT}"

    log_info "Fetching state from: ${state_url}"

    STATE_RESPONSE=$(curl -s \
        --header "PRIVATE-TOKEN: ${GITLAB_TOKEN}" \
        --header "Cache-Control: no-cache, no-store, must-revalidate" \
        --header "Pragma: no-cache" \
        "${state_url}")

    # Check if state exists
    if echo "${STATE_RESPONSE}" | jq -e '.message' &> /dev/null; then
        ERROR_MSG=$(echo "${STATE_RESPONSE}" | jq -r '.message')
        log_error "Failed to fetch Terraform state"
        echo "Error: ${ERROR_MSG}"
        echo ""
        echo "Make sure:"
        echo "  1. Terraform was applied via GitLab CI/CD"
        echo "  2. Environment name is correct: ${ENVIRONMENT}"
        echo "  3. GITLAB_PAT has 'api' scope"
        echo "  4. Project ID is correct: ${PROJECT_ID}"
        echo "  5. GitLab instance URL is correct: ${GITLAB_API_URL}"
        exit 1
    fi

    # Display state update time for verification
    STATE_UPDATED_AT=$(echo "${STATE_RESPONSE}" | jq -r '.updated_at // empty')
    if [[ -n "${STATE_UPDATED_AT}" ]] && [[ "${STATE_UPDATED_AT}" != "null" ]]; then
        log_info "State last updated: ${STATE_UPDATED_AT}"
    fi

    log_success "Terraform state fetched successfully"
}

# Extract outputs from state
extract_outputs() {
    print_header "Extracting Terraform Outputs"

    # Check if outputs are directly in the response (newer GitLab versions)
    DIRECT_OUTPUTS=$(echo "${STATE_RESPONSE}" | jq -r '.outputs // empty')

    if [[ -n "${DIRECT_OUTPUTS}" ]] && [[ "${DIRECT_OUTPUTS}" != "null" ]]; then
        log_info "Outputs found directly in state response (newer GitLab version)"
        OUTPUTS="${DIRECT_OUTPUTS}"
    else
        # Try to get state download URL (older GitLab versions)
        STATE_DOWNLOAD_URL=$(echo "${STATE_RESPONSE}" | jq -r '.links["state-download"] // empty')

        if [[ -z "${STATE_DOWNLOAD_URL}" ]] || [[ "${STATE_DOWNLOAD_URL}" == "null" ]]; then
            log_error "Could not find state download URL in GitLab response"
            echo "Response (first 500 chars):"
            echo "${STATE_RESPONSE}" | jq '.' | head -c 500
            exit 1
        fi

        log_info "Downloading state file from GitLab..."

        # Download state file with cache-busting to ensure fresh data
        STATE_JSON=$(curl -s \
            --header "PRIVATE-TOKEN: ${GITLAB_TOKEN}" \
            --header "Cache-Control: no-cache, no-store, must-revalidate" \
            --header "Pragma: no-cache" \
            "${STATE_DOWNLOAD_URL}")

        # Extract outputs from state
        OUTPUTS=$(echo "${STATE_JSON}" | jq -r '.outputs // empty')
    fi

    if [[ -z "${OUTPUTS}" ]] || [[ "${OUTPUTS}" == "null" ]]; then
        log_warning "No outputs found in Terraform state"
        log_warning "The state file may not have outputs yet"
        echo ""
        echo "This can happen if:"
        echo "  1. Terraform apply failed before creating outputs"
        echo "  2. No outputs are defined in the Terraform configuration"
        echo "  3. Wrong environment selected"
        echo ""
        echo "Available environments in GitLab CI/CD:"
        echo "  - production"
        echo "  - staging"
        exit 1
    fi

    # Convert outputs to JSON format
    OUTPUTS_JSON=$(echo "${OUTPUTS}" | jq 'with_entries(.value = .value.value)')

    # Save to file
    echo "${OUTPUTS_JSON}" | jq '.' > "${OUTPUTS_FILE}"

    log_success "Outputs saved to: ${OUTPUTS_FILE}"

    # Display outputs
    echo ""
    log_info "Terraform Outputs:"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    cat "${OUTPUTS_FILE}" | jq '.'
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
}

# Display extracted IPs
display_ips() {
    print_header "Extracted IPs"

    MASTER_IP=$(jq -r '.master_public_ip | if type == "object" and has("value") then .value elif type == "string" then . else . end' "${OUTPUTS_FILE}")
    WORKER_IPS=$(jq -r '.worker_public_ips | if type == "object" and has("value") then .value elif type == "object" then . else . end' "${OUTPUTS_FILE}")

    echo "Master IP: ${MASTER_IP}"

    WORKER_COUNT=0
    if [[ -n "${WORKER_IPS}" ]] && [[ "${WORKER_IPS}" != "null" ]] && [[ "${WORKER_IPS}" != "{}" ]]; then
        echo "Worker IPs:"
        for key in $(echo "${WORKER_IPS}" | jq -r 'keys[]'); do
            IP=$(echo "${WORKER_IPS}" | jq -r ".[\"${key}\"]")
            if [[ -n "${IP}" ]] && [[ "${IP}" != "null" ]]; then
                echo "  ${key}: ${IP}"
                WORKER_COUNT=$((WORKER_COUNT + 1))
            fi
        done
    else
        echo "Worker IPs: (none - master-only cluster)"
    fi

    echo ""
    log_success "Total nodes: $((1 + WORKER_COUNT))"
}

# Main workflow
main() {
    clear

    print_header "Fetch Terraform Outputs from GitLab"
    echo "This script fetches Terraform outputs from GitLab HTTP backend"
    echo "Environment: ${ENVIRONMENT}"
    echo "GitLab Instance: Auto-detected from git remote"
    echo ""
    read -p "Continue? (y/N) " -n 1 -r
    echo ""

    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        log_info "Cancelled by user"
        exit 0
    fi

    check_prerequisites
    detect_gitlab_info
    fetch_terraform_state
    extract_outputs
    display_ips

    print_header "Next Steps"
    log_success "Terraform outputs fetched successfully!"
    echo ""
    echo "Now you can:"
    echo "  1. Generate Ansible inventory:"
    echo "     cd ansible-ci"
    echo "     ./scripts/generate-inventory-from-terraform.sh"
    echo ""
    echo "  2. Bootstrap Kubernetes:"
    echo "     ansible-playbook -i inventory.ini k8s-bootstrap.yml"
    echo ""
    echo "  3. Deploy applications:"
    echo "     ansible-playbook -i inventory.ini deploy-apps.yml"
    echo ""
}

# Run main
main "$@"
