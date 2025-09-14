#!/usr/bin/env zsh
# Comprehensive deployment orchestrator for Ajasta application
# This script executes all necessary steps to deploy the application either to VM or locally
#
# Usage:
#   ./deploy-all.zsh [OPTIONS]
#
# Options:
#   --mode vm|local|both     Deployment mode (default: vm)
#   --clean                  Clean up before deployment
#   --skip-build            Skip Docker image building
#   --dockerhub-user USER   DockerHub username (default: vladimirryrik)
#   --help                  Show this help message
#
# Environment Variables:
#   DOCKERHUB_USER          DockerHub username
#   SSH_USERNAME            SSH username for VM (default: ajasta)
#   YC_ZONE                 Yandex Cloud zone (default: ru-central1-b)

set -euo pipefail
SCRIPT_DIR=$(cd -- "${0:A:h}" && pwd)
cd "$SCRIPT_DIR"

# Default configuration
MODE="vm"
CLEAN_FIRST=false
SKIP_BUILD=false
DOCKERHUB_USER=${DOCKERHUB_USER:-vladimirryrik}
HELP=false

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Logging functions
log() {
    local level="$1"
    shift
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    case "$level" in
        "INFO")  echo -e "${CYAN}[INFO $timestamp]${NC} $*" ;;
        "SUCCESS") echo -e "${GREEN}[SUCCESS $timestamp]${NC} $*" ;;
        "WARNING") echo -e "${YELLOW}[WARNING $timestamp]${NC} $*" ;;
        "ERROR")   echo -e "${RED}[ERROR $timestamp]${NC} $*" ;;
        "STEP")    echo -e "${PURPLE}[STEP $timestamp]${NC} $*" ;;
    esac
}

# Error handler
error_exit() {
    log "ERROR" "$1"
    exit 1
}

# Progress tracking
CURRENT_STEP=0
TOTAL_STEPS=7

print_progress() {
    local step_name="$1"
    CURRENT_STEP=$((CURRENT_STEP + 1))
    echo ""
    log "STEP" "[$CURRENT_STEP/$TOTAL_STEPS] $step_name"
    echo "=========================================="
}

# Help function
show_help() {
    cat << EOF
🚀 Ajasta Application Deployment Orchestrator

This script automates the complete deployment process for the Ajasta application.
It can deploy to Yandex Cloud VM, run locally with Docker Compose, or both.

USAGE:
    ./deploy-all.zsh [OPTIONS]

OPTIONS:
    --mode vm|local|both    Deployment mode (default: vm)
                           vm    - Deploy to Yandex Cloud VM
                           local - Run locally with Docker Compose
                           both  - Deploy to both VM and locally

    --clean                Clean up resources before deployment
                          For VM: runs destroy-all.zsh
                          For local: runs docker-compose down

    --skip-build          Skip Docker image building step
                         Useful when images are already built

    --dockerhub-user USER DockerHub username (default: vladimirryrik)
                         Used for pulling/pushing container images

    --help               Show this help message

ENVIRONMENT VARIABLES:
    DOCKERHUB_USER       DockerHub username (default: vladimirryrik)
    SSH_USERNAME         SSH username for VM access (default: ajasta)
    YC_ZONE             Yandex Cloud zone (default: ru-central1-b)
    YC_FOLDER_ID        Yandex Cloud folder ID
    YC_CLOUD_ID         Yandex Cloud ID

EXAMPLES:
    # Deploy to VM with default settings
    ./deploy-all.zsh

    # Deploy locally with Docker Compose
    ./deploy-all.zsh --mode local

    # Deploy to both VM and locally
    ./deploy-all.zsh --mode both

    # Clean deployment to VM with custom DockerHub user
    ./deploy-all.zsh --clean --dockerhub-user myuser

    # Deploy locally without rebuilding images
    ./deploy-all.zsh --mode local --skip-build

WORKFLOW (VM mode):
    1. Clean up existing resources (if --clean)
    2. Create basic VM in Yandex Cloud
    3. Check SSH connectivity to VM
    4. Build and push Docker images to DockerHub (if not --skip-build)
    5. Deploy containerized setup to VM
    6. Install and configure containers on VM
    7. Start containers and verify deployment

WORKFLOW (local mode):
    1. Clean up existing containers (if --clean)
    2. Build Docker images locally (if not --skip-build)
    3. Start services with Docker Compose
    4. Verify local deployment

For more information, see README.md
EOF
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --mode)
            MODE="$2"
            if [[ "$MODE" != "vm" && "$MODE" != "local" && "$MODE" != "both" ]]; then
                error_exit "Invalid mode: $MODE. Must be 'vm', 'local', or 'both'"
            fi
            shift 2
            ;;
        --clean)
            CLEAN_FIRST=true
            shift
            ;;
        --skip-build)
            SKIP_BUILD=true
            shift
            ;;
        --dockerhub-user)
            DOCKERHUB_USER="$2"
            shift 2
            ;;
        --help)
            show_help
            exit 0
            ;;
        *)
            error_exit "Unknown option: $1. Use --help for usage information."
            ;;
    esac
done

# Validate prerequisites
check_prerequisites() {
    log "INFO" "Checking prerequisites..."
    
    local missing_deps=()
    
    # Common dependencies
    command -v docker >/dev/null 2>&1 || missing_deps+=("docker")
    command -v jq >/dev/null 2>&1 || missing_deps+=("jq")
    
    # VM mode dependencies
    if [[ "$MODE" == "vm" || "$MODE" == "both" ]]; then
        command -v yc >/dev/null 2>&1 || missing_deps+=("yc (Yandex Cloud CLI)")
        command -v ssh >/dev/null 2>&1 || missing_deps+=("ssh")
    fi
    
    # Local mode dependencies
    if [[ "$MODE" == "local" || "$MODE" == "both" ]]; then
        command -v docker-compose >/dev/null 2>&1 || missing_deps+=("docker-compose")
    fi
    
    if [[ ${#missing_deps[@]} -gt 0 ]]; then
        log "ERROR" "Missing required dependencies:"
        for dep in "${missing_deps[@]}"; do
            echo "  - $dep"
        done
        error_exit "Please install missing dependencies and try again"
    fi
    
    log "SUCCESS" "All prerequisites satisfied"
}

# VM deployment functions
deploy_to_vm() {
    log "INFO" "Starting VM deployment with DOCKERHUB_USER=$DOCKERHUB_USER"
    
    # Step 1: Clean up (if requested)
    if [[ "$CLEAN_FIRST" == "true" ]]; then
        print_progress "Cleaning up existing Yandex Cloud resources"
        ./destroy-all.zsh || log "WARNING" "Cleanup completed with warnings"
        sleep 5  # Give time for resources to be fully removed
    fi
    
    # Step 2: Create basic VM
    print_progress "Creating basic VM in Yandex Cloud"
    ./main.zsh || error_exit "Failed to create basic VM"
    
    # Step 3: Check SSH connectivity
    print_progress "Checking SSH connectivity to VM"
    local ssh_attempts=0
    local max_ssh_attempts=5
    while [[ $ssh_attempts -lt $max_ssh_attempts ]]; do
        if ./ssh-ajasta.zsh "echo 'SSH connection successful'"; then
            log "SUCCESS" "SSH connection established"
            break
        else
            ssh_attempts=$((ssh_attempts + 1))
            log "WARNING" "SSH attempt $ssh_attempts/$max_ssh_attempts failed, retrying in 10 seconds..."
            sleep 10
        fi
    done
    
    if [[ $ssh_attempts -eq $max_ssh_attempts ]]; then
        error_exit "Failed to establish SSH connection after $max_ssh_attempts attempts"
    fi
    
    # Step 4: Build and push Docker images (if not skipped)
    if [[ "$SKIP_BUILD" == "false" ]]; then
        print_progress "Building and pushing Docker images to DockerHub"
        DOCKERHUB_USER="$DOCKERHUB_USER" ./build-and-push-images.zsh || error_exit "Failed to build and push images"
    else
        log "INFO" "Skipping Docker image building as requested"
    fi
    
    # Step 5: Deploy containerized setup
    print_progress "Deploying containerized setup to VM"
    DOCKERHUB_USER="$DOCKERHUB_USER" ./main-containerized.zsh || error_exit "Failed to deploy containerized setup"
    
    # Wait for VM to be ready
    log "INFO" "Waiting 60 seconds for VM initialization..."
    sleep 60
    
    # Step 6: Install and configure containers
    print_progress "Installing and configuring containers on VM"
    DOCKERHUB_USER="$DOCKERHUB_USER" ./repair-containerized-vm.zsh || error_exit "Failed to install containers"
    
    # Step 7: Start containers and verify
    print_progress "Starting containers and verifying deployment"
    ./ssh-ajasta.zsh '/opt/ajasta/start-containers.sh' || log "WARNING" "Container start completed with warnings"
    
    # Wait for containers to start
    log "INFO" "Waiting 30 seconds for containers to start..."
    sleep 30
    
    # Verify deployment
    log "INFO" "Verifying VM deployment..."
    ./ssh-ajasta.zsh '/opt/ajasta/status.sh' || log "WARNING" "Status check completed with warnings"
    
    # Get VM IP and show access URLs
    local vm_ip
    vm_ip=$(./ssh-ajasta.zsh "curl -s http://ifconfig.me" 2>/dev/null || echo "UNKNOWN")
    
    if [[ "$vm_ip" != "UNKNOWN" ]]; then
        log "SUCCESS" "VM deployment completed successfully!"
        echo ""
        echo "🌐 Your application is accessible at:"
        echo "   Frontend (React):    http://$vm_ip"
        echo "   Backend API:         http://$vm_ip:8090"
        echo "   PostgreSQL:          $vm_ip:15432"
        echo ""
        echo "🔧 Management commands:"
        echo "   SSH to VM:           ./ssh-ajasta.zsh"
        echo "   Check containers:    ./ssh-ajasta.zsh '/opt/ajasta/status.sh'"
        echo "   Restart containers:  ./ssh-ajasta.zsh 'sudo systemctl restart ajasta-containers'"
    else
        log "WARNING" "VM deployment completed but could not determine IP address"
    fi
}

# Local deployment functions
deploy_locally() {
    log "INFO" "Starting local deployment with Docker Compose"
    
    # Step 1: Clean up (if requested)
    if [[ "$CLEAN_FIRST" == "true" ]]; then
        print_progress "Cleaning up existing local containers"
        cd ..
        docker-compose down -v 2>/dev/null || log "WARNING" "Local cleanup completed with warnings"
        cd scripts
    fi
    
    # Step 2: Build images locally (if not skipped)
    if [[ "$SKIP_BUILD" == "false" ]]; then
        print_progress "Building Docker images locally"
        DOCKERHUB_USER="$DOCKERHUB_USER" ./build-and-push-images.zsh --local-only || error_exit "Failed to build images locally"
    else
        log "INFO" "Skipping Docker image building as requested"
    fi
    
    # Step 3: Start with Docker Compose
    print_progress "Starting services with Docker Compose"
    cd ..
    DOCKERHUB_USER="$DOCKERHUB_USER" docker-compose up -d || error_exit "Failed to start services with Docker Compose"
    
    # Step 4: Verify local deployment
    print_progress "Verifying local deployment"
    
    log "INFO" "Waiting 30 seconds for services to start..."
    sleep 30
    
    # Check service status
    docker-compose ps
    
    log "SUCCESS" "Local deployment completed successfully!"
    echo ""
    echo "🌐 Your application is accessible at:"
    echo "   Frontend (React):    http://localhost:3000"
    echo "   Backend API:         http://localhost:8090"
    echo "   PostgreSQL:          localhost:15432"
    echo ""
    echo "🔧 Management commands:"
    echo "   View logs:           docker-compose logs -f"
    echo "   Restart services:    docker-compose restart"
    echo "   Stop services:       docker-compose down"
    echo "   View status:         docker-compose ps"
    
    cd scripts
}

# Main execution
main() {
    echo ""
    echo "🚀 Ajasta Application Deployment Orchestrator"
    echo "=============================================="
    echo ""
    log "INFO" "Deployment mode: $MODE"
    log "INFO" "DockerHub user: $DOCKERHUB_USER"
    log "INFO" "Clean first: $CLEAN_FIRST"
    log "INFO" "Skip build: $SKIP_BUILD"
    echo ""
    
    check_prerequisites
    
    case "$MODE" in
        "vm")
            deploy_to_vm
            ;;
        "local")
            deploy_locally
            ;;
        "both")
            log "INFO" "Deploying to both VM and locally..."
            deploy_to_vm
            echo ""
            log "INFO" "VM deployment completed, starting local deployment..."
            echo ""
            deploy_locally
            ;;
    esac
    
    echo ""
    log "SUCCESS" "🎉 Deployment orchestration completed successfully!"
    echo ""
}

# Execute main function
main "$@"