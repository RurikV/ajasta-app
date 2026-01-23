#!/bin/bash
# Master Deployment Orchestrator for Ajasta Multi-Environment Deployments
# This script orchestrates the complete deployment process for staging or production
#
# Usage:
#   ./deploy-to-environment.sh <environment> [--step <N>] [--playbook <NN>] [--yes] [-v|-vv|-vvv]
#
# Examples:
#   ./deploy-to-environment.sh staging
#   ./deploy-to-environment.sh production --step 3
#   ./deploy-to-environment.sh staging --playbook 03        # Runs 03→04→05→...→31 (all from 03)
#   ./deploy-to-environment.sh production --playbook 19 -vv  # Runs 19→31 with verbose
#   ./deploy-to-environment.sh staging -vv --yes           # Full deployment, no prompts
#
# Environment Deployment Steps:
#   1. Generate Ansible inventory from Yandex Cloud
#   2. Setup Kubernetes cluster (k8s-cluster-setup.sh)
#   3. Deploy CloudNativePG operator and PostgreSQL cluster
#   4. Deploy Ajasta application (deploy-ajasta.sh)
#   5. Fix connection timeout issues
#
# Playbook Mode (--playbook <NN>):
#   - Starts from playbook <NN>
#   - Executes playbook <NN> and ALL subsequent playbooks in sequence
#   - Continues even if a playbook fails (tracks errors)
#   - Example: --playbook 03 runs: 03, 04, 05, 06, 09, 10, 13, 14, 19, 31
#
# Playbook Execution Order:
#   01  → Prepare nodes for Kubernetes
#   02  → Initialize Kubernetes control plane
#   03  → Install Cilium CNI
#   04  → Join worker nodes
#   05  → Install containerd and nerdctl
#   06  → Deploy test applications
#   09  → Install Helm package manager
#   10  → Deploy Kubernetes Dashboard
#   13  → Deploy Ingress NGINX Controller
#   14  → Deploy Longhorn Storage
#   19  → Deploy CloudNativePG and PostgreSQL
#   31  → Fix connection timeout issues

# NOTE: Script continues on error to allow partial deployment

# Initialize failed steps array
FAILED_STEPS=()

# Playbook number mapping
declare -A PLAYBOOKS
PLAYBOOKS[01]="01-prepare-nodes.yml"
PLAYBOOKS[02]="02-init-master.yml"
PLAYBOOKS[03]="03-install-cilium.yml"
PLAYBOOKS[04]="04-join-workers.yml"
PLAYBOOKS[05]="05-install-containerd-nerdctl.yml"
PLAYBOOKS[06]="06-deploy-test-app.yml"
PLAYBOOKS[09]="09-install-helm-binary.yml"
PLAYBOOKS[10]="10-deploy-kubernetes-dashboard.yml"
PLAYBOOKS[13]="13-deploy-ingress-nginx-controller.yml"
PLAYBOOKS[14]="14-deploy-longhorn-storage.yml"
PLAYBOOKS[19]="19-deploy-cloudnativepg.yml"
PLAYBOOKS[31]="31-fix-connection-timeout-complete.yml"

# Playbook execution order (sorted by numeric key)
PLAYBOOK_ORDER=(
    "01"
    "02"
    "03"
    "04"
    "05"
    "06"
    "09"
    "10"
    "13"
    "14"
    "19"
    "31"
)

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
NC='\033[0m' # No Color

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
ANSIBLE_K8S_DIR="${PROJECT_ROOT}/ansible/k8s"
ANSIBLE_APP_DIR="${PROJECT_ROOT}/ansible/ajasta-app"
ENVIRONMENTS_DIR="${PROJECT_ROOT}/environments"

# Default values
ENVIRONMENT=""
START_STEP=1
PLAYBOOK_NUM=""
VERBOSITY=""
DRY_RUN=false
SKIP_CONFIRMATION=false

# Functions
print_header() {
    echo ""
    echo -e "${MAGENTA}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${MAGENTA}  $1${NC}"
    echo -e "${MAGENTA}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""
}

print_step() {
    local step_num=$1
    local step_name=$2

    if [ "$step_num" -lt "$START_STEP" ]; then
        echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
        echo -e "${CYAN}SKIPPED: Step $step_num - $step_name${NC}"
        echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    else
        echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
        echo -e "${GREEN}STEP $step_num: $step_name${NC}"
        echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    fi
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ ERROR: $1${NC}"
    # Don't exit, just print error and continue
    FAILED_STEPS+=("$1")
}

run_playbook() {
    local playbook_num="$1"
    local playbook_file="${PLAYBOOKS[$playbook_num]}"

    if [ -z "$playbook_file" ]; then
        print_error "Unknown playbook number: $playbook_num"
        echo ""
        echo "Available playbooks:"
        for num in "${!PLAYBOOKS[@]}"; do
            echo "  $num → ${PLAYBOOKS[$num]}"
        done | sort
        return 1
    fi

    print_header "RUNNING PLAYBOOK: $playbook_file"
    echo -e "${BLUE}Playbook Number:${NC} ${YELLOW}${playbook_num}${NC}"
    echo -e "${BLUE}Environment:${NC} ${YELLOW}$(to_upper "$ENVIRONMENT")${NC}"
    if [ -n "$VERBOSITY" ]; then
        echo -e "${BLUE}Verbosity:${NC} ${YELLOW}${VERBOSITY}${NC}"
    fi
    echo ""

    cd "${ANSIBLE_K8S_DIR}"

    print_info "Executing: ansible-playbook -i inventory.ini $playbook_file ${VERBOSITY}"
    echo ""

    if ansible-playbook -i inventory.ini "$playbook_file" ${VERBOSITY}; then
        print_success "Playbook $playbook_num ($playbook_file) completed successfully"
        return 0
    else
        print_error "Playbook $playbook_num ($playbook_file) failed"
        return 1
    fi
}

run_playbooks_from() {
    local start_playbook="$1"
    local found_start=false
    local playbooks_to_run=()

    # Find the starting playbook and collect all subsequent playbooks
    for playbook_num in "${PLAYBOOK_ORDER[@]}"; do
        if [ "$playbook_num" = "$start_playbook" ]; then
            found_start=true
        fi

        if [ "$found_start" = true ]; then
            playbooks_to_run+=("$playbook_num")
        fi
    done

    if [ ${#playbooks_to_run[@]} -eq 0 ]; then
        print_error "Playbook $start_playbook not found in execution order"
        return 1
    fi

    print_header "PLAYBOOK SEQUENCE EXECUTION"
    echo -e "${GREEN}Starting from:${NC} ${YELLOW}${start_playbook}${NC} (${PLAYBOOKS[$start_playbook]})"
    echo -e "${GREEN}Total playbooks to run:${NC} ${YELLOW}${#playbooks_to_run[@]}${NC}"
    echo ""
    echo -e "${BLUE}Playbook sequence:${NC}"
    for i in "${!playbooks_to_run[@]}"; do
        local num="${playbooks_to_run[$i]}"
        if [ "$i" -eq 0 ]; then
            echo "  ${GREEN}→${NC} $num: ${PLAYBOOKS[$num]} ${GREEN}[START]${NC}"
        else
            echo "  ${CYAN}→${NC} $num: ${PLAYBOOKS[$num]}"
        fi
    done
    echo ""

    # Check if user wants to see the sequence only (dry run for playbook mode)
    if [ "$DRY_RUN" = true ]; then
        print_info "Dry run mode - would execute ${#playbooks_to_run[@]} playbooks"
        return 0
    fi

    # Run each playbook in sequence
    local completed=0
    local failed=0

    for playbook_num in "${playbooks_to_run[@]}"; do
        echo ""
        echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
        echo -e "${BLUE}[$((completed + failed + 1))/${#playbooks_to_run[@]}] Running playbook: ${PLAYBOOKS[$playbook_num]}${NC}"
        echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
        echo ""

        if run_playbook "$playbook_num"; then
            ((completed++))
        else
            ((failed++))
            print_warning "Playbook failed, continuing with next playbook..."
        fi
    done

    # Show summary
    echo ""
    print_header "PLAYBOOK SEQUENCE COMPLETE"
    echo -e "${GREEN}Completed:${NC} $completed"
    echo -e "${RED}Failed:${NC} $failed"
    echo -e "${CYAN}Total:${NC} $((completed + failed))"
    echo ""

    if [ $failed -gt 0 ]; then
        print_warning "Sequence completed with $failed error(s)"
        return 1
    else
        print_success "All playbooks in sequence completed successfully!"
        return 0
    fi
}

print_warning() {
    echo -e "${YELLOW}⚠️  WARNING: $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

show_usage() {
    cat << EOF
${MAGENTA}Ajasta Multi-Environment Deployment Orchestrator${NC}

${GREEN}Usage:${NC}
  $0 <environment> [options]

${GREEN}Arguments:${NC}
  environment        Target environment (staging or production)

${GREEN}Options:${NC}
  --step <N>         Start deployment from step N (1-5)
  --playbook <NN>    Start from playbook <NN> and run ALL subsequent playbooks
  -v, -vv, -vvv      Ansible verbosity level
  --dry-run          Show what would be done without executing
  --yes, -y          Skip confirmation prompts (run automatically)
  --help, -h         Show this help message

${GREEN}Environments:${NC}
  staging            Deploy to staging environment
  production         Deploy to production environment

${GREEN}Deployment Steps:${NC}
  1. Generate Ansible inventory from Yandex Cloud VMs
  2. Setup Kubernetes cluster (components, networking, storage)
  3. Deploy CloudNativePG operator and PostgreSQL cluster
  4. Deploy Ajasta application (backend, frontend, ingress)
  5. Fix connection timeout issues

${GREEN}Available Playbooks:${NC}
  01  → Prepare nodes for Kubernetes
  02  → Initialize Kubernetes control plane
  03  → Install Cilium CNI
  04  → Join worker nodes
  05  → Install containerd and nerdctl
  06  → Deploy test applications
  09  → Install Helm package manager
  10  → Deploy Kubernetes Dashboard
  13  → Deploy Ingress NGINX Controller
  14  → Deploy Longhorn Storage
  19  → Deploy CloudNativePG and PostgreSQL
  31  → Fix connection timeout issues

${GREEN}Playbook Mode:${NC}
  When using --playbook <NN>, the script will:
  - Start from playbook <NN>
  - Execute playbook <NN> and ALL subsequent playbooks in sequence
  - Continue even if a playbook fails (tracks errors)
  - Example: --playbook 03 runs: 03, 04, 05, 06, 09, 10, 13, 14, 19, 31

${GREEN}Examples:${NC}
  $0 staging                                    # Deploy all steps to staging
  $0 production -vv                             # Deploy to production with verbose output
  $0 staging --step 3                           # Start from step 3 (skip 1-2)
  $0 staging --playbook 03                      # Run playbooks 03→04→05→...→31 (all from 03)
  $0 production --playbook 19 -vv               # Run playbooks 19→31 with verbose output
  $0 staging --playbook 10 --yes                # Run playbooks 10→13→14→19→31 without prompts
  $0 production --step 4 -vvv --yes             # Start from step 4, verbose, no prompts

${GREEN}Environment Configuration:${NC}
  Staging:
    - Domain: staging.ajasta.top
    - VM Prefix: ajasta-staging
    - Resources: Minimal (1 worker)
    - TLS: Let's Encrypt Staging

  Production:
    - Domain: ajasta.top
    - VM Prefix: ajasta-prod
    - Resources: Full (3 workers, HA database)
    - TLS: Let's Encrypt Production

EOF
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        staging|production)
            ENVIRONMENT="$1"
            shift
            ;;
        --step)
            START_STEP="$2"
            shift 2
            ;;
        --playbook)
            PLAYBOOK_NUM="$2"
            shift 2
            ;;
        -v|--verbose)
            VERBOSITY="-v"
            shift
            ;;
        -vv)
            VERBOSITY="-vv"
            shift
            ;;
        -vvv)
            VERBOSITY="-vvv"
            shift
            ;;
        --dry-run)
            DRY_RUN=true
            shift
            ;;
        --yes|-y)
            SKIP_CONFIRMATION=true
            shift
            ;;
        --help|-h)
            show_usage
            exit 0
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            echo "Use --help for usage information"
            exit 1
            ;;
    esac
done

# Validate environment
if [ -z "$ENVIRONMENT" ]; then
    print_error "Environment not specified!"
    echo ""
    echo "Usage: $0 <environment> [options]"
    echo "Run --help for more information"
    exit 1
fi

if [[ ! "$ENVIRONMENT" =~ ^(staging|production)$ ]]; then
    print_error "Invalid environment: $ENVIRONMENT"
    echo "Valid environments: staging, production"
    exit 1
fi

# Load environment configuration
ENV_CONFIG_FILE="${ENVIRONMENTS_DIR}/${ENVIRONMENT}/config.yaml"
if [ ! -f "$ENV_CONFIG_FILE" ]; then
    print_error "Environment configuration not found: $ENV_CONFIG_FILE"
    exit 1
fi

# Helper function to convert string to uppercase (bash 3 compatible)
to_upper() {
    echo "$1" | tr '[:lower:]' '[:upper:]'
}

# ==============================================================
# PLAYBOOK MODE: Run playbook and all subsequent playbooks
# ==============================================================
if [ -n "$PLAYBOOK_NUM" ]; then
    clear
    print_header "AJASTA PLAYBOOK SEQUENCE EXECUTION"
    echo -e "${GREEN}Environment:${NC} ${YELLOW}$(to_upper "$ENVIRONMENT")${NC}"
    echo -e "${GREEN}Start Playbook:${NC} ${YELLOW}${PLAYBOOK_NUM}${NC} (${PLAYBOOKS[$PLAYBOOK_NUM]})"
    echo -e "${GREEN}Mode:${NC} ${YELLOW}Sequential${NC} (run this and all subsequent playbooks)"
    if [ -n "$VERBOSITY" ]; then
        echo -e "${GREEN}Verbosity:${NC} ${YELLOW}${VERBOSITY}${NC}"
    fi
    echo ""

    # Show what will be executed
    print_info "The following playbooks will be executed:"
    local found_start=false
    local count=0
    for playbook_num in "${PLAYBOOK_ORDER[@]}"; do
        if [ "$playbook_num" = "$PLAYBOOK_NUM" ]; then
            found_start=true
        fi
        if [ "$found_start" = true ]; then
            ((count++))
            if [ "$count" -eq 1 ]; then
                echo "  ${GREEN}$count.${NC} ${GREEN}$playbook_num${NC} → ${PLAYBOOKS[$playbook_num]} ${GREEN}[START HERE]${NC}"
            else
                echo "  ${CYAN}$count.${NC} $playbook_num → ${PLAYBOOKS[$playbook_num]}"
            fi
        fi
    done
    echo ""
    echo -e "${YELLOW}Total: $count playbook(s) will be executed${NC}"
    echo ""

    # Skip confirmation if --yes was used
    if [ "$SKIP_CONFIRMATION" = false ]; then
        read -p "Execute playbook sequence starting from ${PLAYBOOK_NUM}? (y/n) " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            print_error "Playbook sequence cancelled"
            exit 0
        fi
    fi

    echo ""
    print_info "Starting playbook sequence execution..."
    echo ""

    # Run the playbook sequence
    if run_playbooks_from "$PLAYBOOK_NUM"; then
        echo ""
        print_header "PLAYBOOK SEQUENCE COMPLETE"
        print_success "All playbooks from ${PLAYBOOK_NUM} onward executed successfully!"
        echo ""

        if [ ${#FAILED_STEPS[@]} -gt 0 ]; then
            echo -e "${YELLOW}⚠️  Completed with ${#FAILED_STEPS[@]} warning(s)${NC}"
        fi

        echo ""
        echo "Next steps:"
        echo "  - Run another sequence: $0 ${ENVIRONMENT} --playbook <NN>"
        echo "  - Run full deployment: $0 ${ENVIRONMENT}"
        echo "  - Run from specific step: $0 ${ENVIRONMENT} --step <N>"
        echo ""
    else
        echo ""
        print_header "PLAYBOOK SEQUENCE COMPLETED WITH ERRORS"
        print_error "Some playbooks in the sequence failed!"
        echo ""
        echo "Check the output above for error details."
        echo ""
        echo "Recovery options:"
        echo "  - Re-run from same playbook: $0 ${ENVIRONMENT} --playbook ${PLAYBOOK_NUM}"
        echo "  - Re-run from failed playbook: $0 ${ENVIRONMENT} --playbook <NN>"
        echo "  - Continue from next playbook: Find the last successful playbook and add 1"
        echo ""
    fi

    exit 0
fi

# ==============================================================
# STEP-BASED DEPLOYMENT MODE
# ==============================================================

# Display deployment banner
clear
print_header "AJASTA MULTI-ENVIRONMENT DEPLOYMENT"
echo -e "${GREEN}Environment:${NC} ${YELLOW}$(to_upper "$ENVIRONMENT")${NC}"
echo -e "${GREEN}Start Step:${NC} ${YELLOW}${START_STEP}${NC}"
if [ -n "$VERBOSITY" ]; then
    echo -e "${GREEN}Verbosity:${NC} ${YELLOW}${VERBOSITY}${NC}"
fi
if [ "$DRY_RUN" = true ]; then
    echo -e "${YELLOW}DRY RUN MODE - No changes will be made${NC}"
fi
echo ""

# Show environment details
print_info "Environment Configuration:"
echo "  - Config File: $ENV_CONFIG_FILE"
echo "  - VM Prefix: ajasta-${ENVIRONMENT}"
echo "  - Terraform Prefix: ajasta-${ENVIRONMENT}"

# Show deployment plan
echo ""
print_info "Deployment Plan:"

if [ "$START_STEP" -le 1 ]; then
    echo "  ${GREEN}✓${NC} Step 1: Generate Ansible inventory from Yandex Cloud"
else
    echo "  ${CYAN}⊘${NC} Step 1: Generate Ansible inventory (skipped)"
fi

if [ "$START_STEP" -le 2 ]; then
    echo "  ${GREEN}✓${NC} Step 2: Setup Kubernetes cluster"
else
    echo "  ${CYAN}⊘${NC} Step 2: Setup Kubernetes cluster (skipped)"
fi

if [ "$START_STEP" -le 3 ]; then
    echo "  ${GREEN}✓${NC} Step 3: Deploy CloudNativePG and PostgreSQL"
else
    echo "  ${CYAN}⊘${NC} Step 3: Deploy CloudNativePG (skipped)"
fi

if [ "$START_STEP" -le 4 ]; then
    echo "  ${GREEN}✓${NC} Step 4: Deploy Ajasta application"
else
    echo "  ${CYAN}⊘${NC} Step 4: Deploy Ajasta application (skipped)"
fi

if [ "$START_STEP" -le 5 ]; then
    echo "  ${GREEN}✓${NC} Step 5: Fix connection timeout issues"
else
    echo "  ${CYAN}⊘${NC} Step 5: Fix connection timeout (skipped)"
fi

echo ""

# Safety check for production
if [ "$ENVIRONMENT" = "production" ] && [ "$START_STEP" -le 2 ] && [ "$SKIP_CONFIRMATION" = false ]; then
    print_warning "You are about to deploy to PRODUCTION environment!"
    echo ""
    echo "This will:"
    echo "  - Generate inventory from production VMs"
    echo "  - Setup/modify Kubernetes cluster"
    echo "  - Deploy application to production"
    echo ""
    read -p "Type 'production' to confirm: " confirmation
    if [ "$confirmation" != "production" ]; then
        print_error "Deployment cancelled"
        exit 1
    fi
    echo ""
fi

# Dry run check
if [ "$DRY_RUN" = true ]; then
    print_info "Dry run mode - would execute the following steps:"
    echo ""
    print_step 1 "Generate Ansible inventory from Yandex Cloud"
    echo "  Command: ${ANSIBLE_K8S_DIR}/generate-inventory-from-yc.sh"
    echo ""
    print_step 2 "Setup Kubernetes cluster"
    echo "  Command: ${ANSIBLE_K8S_DIR}/k8s-cluster-setup.sh ${START_STEP} ${VERBOSITY}"
    echo ""
    print_step 3 "Deploy CloudNativePG and PostgreSQL"
    echo "  Command: ansible-playbook -i inventory.ini 19-deploy-cloudnativepg.yml ${VERBOSITY}"
    echo ""
    print_step 4 "Deploy Ajasta application"
    echo "  Command: ${ANSIBLE_APP_DIR}/deploy-ajasta.sh ${START_STEP} ${VERBOSITY}"
    echo ""
    print_step 5 "Fix connection timeout issues"
    echo "  Command: cd ${ANSIBLE_APP_DIR} && ansible-playbook -i ../k8s/inventory.ini 31-fix-connection-timeout-complete.yml ${VERBOSITY}"
    echo ""
    print_success "Dry run complete"
    exit 0
fi

# Final confirmation
if [ "$SKIP_CONFIRMATION" = false ]; then
    read -p "Proceed with deployment? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        print_error "Deployment cancelled"
        exit 0
    fi
fi

echo ""
print_info "Starting deployment to ${ENVIRONMENT}..."
echo ""

# ==============================================================

# STEP 1: Generate Ansible inventory from Yandex Cloud
# ==============================================================
if [ "$START_STEP" -le 1 ]; then
    print_step 1 "Generate Ansible inventory from Yandex Cloud"

    print_info "Running inventory generation script..."
    cd "${ANSIBLE_K8S_DIR}"

    if bash generate-inventory-from-yc.sh; then
        print_success "Ansible inventory generated successfully"
    else
        print_error "Failed to generate Ansible inventory"
        echo "Check that:"
        echo "  1. Yandex Cloud CLI (yc) is installed and authenticated"
        echo "  2. VMs are running in Yandex Cloud"
        echo "  3. VMs have prefix: ajasta-${ENVIRONMENT}"
        exit 1
    fi
else
    print_step 1 "Generate Ansible inventory from Yandex Cloud"
    print_info "Skipping (step 1 < START_STEP)"
fi

# ==============================================================
# STEP 2: Setup Kubernetes cluster
# ==============================================================
if [ "$START_STEP" -le 2 ]; then
    print_step 2 "Setup Kubernetes cluster"

    print_info "Running k8s-cluster-setup script..."
    cd "${ANSIBLE_K8S_DIR}"

    if bash k8s-cluster-setup.sh 1 ${VERBOSITY}; then
        print_success "Kubernetes cluster setup completed"
    else
        print_error "Kubernetes cluster setup failed"
        exit 1
    fi
else
    print_step 2 "Setup Kubernetes cluster"
    print_info "Skipping (step 2 < START_STEP)"
fi

# ==============================================================
# STEP 3: Deploy CloudNativePG and PostgreSQL cluster
# ==============================================================
if [ "$START_STEP" -le 3 ]; then
    print_step 3 "Deploy CloudNativePG and PostgreSQL cluster"

    print_info "Deploying CloudNativePG operator..."
    cd "${ANSIBLE_K8S_DIR}"

    if ansible-playbook -i inventory.ini 19-deploy-cloudnativepg.yml ${VERBOSITY}; then
        print_success "CloudNativePG and PostgreSQL deployed successfully"
    else
        print_error "CloudNativePG deployment failed"
        exit 1
    fi
else
    print_step 3 "Deploy CloudNativePG and PostgreSQL cluster"
    print_info "Skipping (step 3 < START_STEP)"
fi

# ==============================================================
# STEP 4: Deploy Ajasta application
# ==============================================================
if [ "$START_STEP" -le 4 ]; then
    print_step 4 "Deploy Ajasta application"

    print_info "Running deploy-ajasta script..."
    cd "${ANSIBLE_APP_DIR}"

    # Pass environment to deploy-ajasta.sh
    export DEPLOY_ENVIRONMENT="${ENVIRONMENT}"

    if bash deploy-ajasta.sh 1 ${VERBOSITY}; then
        print_success "Ajasta application deployed successfully"
    else
        print_error "Ajasta application deployment failed"
        exit 1
    fi
else
    print_step 4 "Deploy Ajasta application"
    print_info "Skipping (step 4 < START_STEP)"
fi

# ==============================================================
# STEP 5: Fix connection timeout issues
# ==============================================================
if [ "$START_STEP" -le 5 ]; then
    print_step 5 "Fix connection timeout issues"

    print_info "Running connection timeout fix playbook..."
    cd "${ANSIBLE_APP_DIR}"

    if ansible-playbook -i ../k8s/inventory.ini 31-fix-connection-timeout-complete.yml ${VERBOSITY}; then
        print_success "Connection timeout issues resolved"
    else
        print_warning "Connection timeout fix completed with warnings"
        echo "This is often non-critical. Check the output above."
    fi
else
    print_step 5 "Fix connection timeout issues"
    print_info "Skipping (step 5 < START_STEP)"
fi

# ==============================================================
# DEPLOYMENT COMPLETE
# ==============================================================
print_header "DEPLOYMENT COMPLETE"

echo ""

# Check if there were any failures
if [ ${#FAILED_STEPS[@]} -gt 0 ]; then
    echo -e "${YELLOW}╔════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${YELLOW}║   DEPLOYMENT COMPLETED WITH WARNINGS                         ║${NC}"
    echo -e "${YELLOW}╚════════════════════════════════════════════════════════════════╝${NC}"
    echo ""
    echo -e "${RED}Failed Steps:${NC}"
    for step in "${FAILED_STEPS[@]}"; do
        echo -e "${RED}  ✗ $step${NC}"
    done
    echo ""
    echo -e "${GREEN}✓ Deployment to ${YELLOW}$(to_upper "$ENVIRONMENT")${GREEN} completed with ${#FAILED_STEPS[@]} warning(s)${NC}"
else
    echo -e "${GREEN}╔════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║   DEPLOYMENT COMPLETED SUCCESSFULLY                            ║${NC}"
    echo -e "${GREEN}╚════════════════════════════════════════════════════════════════╝${NC}"
    echo ""
    echo -e "${GREEN}✓ Deployment to ${YELLOW}$(to_upper "$ENVIRONMENT")${GREEN} completed successfully!${NC}"
fi

echo ""

# Show environment-specific information
if [ "$ENVIRONMENT" = "staging" ]; then
    echo "Staging Environment:"
    echo "  - URL: https://staging.ajasta.top"
    echo "  - Backend API: https://staging.ajasta.top/api"
    echo ""
else
    echo "Production Environment:"
    echo "  - URL: https://ajasta.top"
    echo "  - Backend API: https://ajasta.top/api"
    echo ""
fi

echo "Next Steps:"
echo "  1. Verify application is accessible"
echo "  2. Check pod status: kubectl get pods -n ajasta-${ENVIRONMENT}"
echo "  3. View logs: kubectl logs -n ajasta-${ENVIRONMENT} -l component=backend -f"
echo "  4. Monitor ingress: kubectl get ingress -n ajasta-${ENVIRONMENT}"
echo ""

# Show available playbooks if there were failures
if [ ${#FAILED_STEPS[@]} -gt 0 ]; then
    echo "Recovery Options:"
    echo "  - Re-run failed playbook: $0 ${ENVIRONMENT} --playbook <NN>"
    echo "  - Continue from specific step: $0 ${ENVIRONMENT} --step <N>"
    echo ""
    echo "Available playbooks:"
    for num in "${!PLAYBOOKS[@]}"; do
        echo "  - $num: ${PLAYBOOKS[$num]}"
    done | sort
    echo ""
fi

print_success "All done!"
echo ""
