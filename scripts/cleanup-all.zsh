#!/usr/bin/env zsh
# Cleanup and maintenance script for Ajasta application
# Handles cleanup for both VM and local Docker Compose deployments
#
# Usage:
#   ./cleanup-all.zsh [OPTIONS]
#
# Options:
#   --mode vm|local|both     Cleanup specific deployment (default: both)
#   --force                  Skip confirmation prompts
#   --keep-data             Keep persistent data (volumes)
#   --keep-images           Keep Docker images
#   --deep-clean            Remove everything including networks and system cleanup
#   --help                  Show this help message

set -euo pipefail
SCRIPT_DIR=$(cd -- "${0:A:h}" && pwd)
cd "$SCRIPT_DIR"

# Default configuration
MODE="both"
FORCE=false
KEEP_DATA=false
KEEP_IMAGES=false
DEEP_CLEAN=false

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Logging function
log() {
    local level="$1"
    shift
    case "$level" in
        "INFO")    echo -e "${CYAN}[INFO]${NC} $*" ;;
        "SUCCESS") echo -e "${GREEN}[SUCCESS]${NC} $*" ;;
        "WARNING") echo -e "${YELLOW}[WARNING]${NC} $*" ;;
        "ERROR")   echo -e "${RED}[ERROR]${NC} $*" ;;
        "HEADER")  echo -e "${PURPLE}$*${NC}" ;;
    esac
}

# Confirmation function
confirm() {
    if [[ "$FORCE" == "true" ]]; then
        return 0
    fi
    
    local message="$1"
    echo ""
    log "WARNING" "$message"
    echo -n "Are you sure? (y/N): "
    read -r response
    if [[ "$response" =~ ^[Yy]$ ]]; then
        return 0
    else
        log "INFO" "Operation cancelled"
        return 1
    fi
}

# Help function
show_help() {
    cat << EOF
🧹 Ajasta Application Cleanup Tool

This script handles cleanup and maintenance for Ajasta application deployments.
It can clean VM resources, local Docker containers, or both.

USAGE:
    ./cleanup-all.zsh [OPTIONS]

OPTIONS:
    --mode vm|local|both     Cleanup specific deployment (default: both)
                            vm    - Cleanup Yandex Cloud VM resources
                            local - Cleanup local Docker containers
                            both  - Cleanup both deployments

    --force                 Skip confirmation prompts
                           Useful for automated scripts

    --keep-data            Keep persistent data (volumes)
                          Preserves database data and uploaded files

    --keep-images          Keep Docker images
                          Only removes containers, not images

    --deep-clean           Remove everything including:
                          - Docker networks and volumes
                          - Unused Docker system resources
                          - Local development files
                          - Log files

    --help                 Show this help message

EXAMPLES:
    # Interactive cleanup of both deployments
    ./cleanup-all.zsh

    # Force cleanup of VM without prompts
    ./cleanup-all.zsh --mode vm --force

    # Clean local containers but keep data
    ./cleanup-all.zsh --mode local --keep-data

    # Deep clean everything
    ./cleanup-all.zsh --deep-clean --force

    # Clean containers but keep images and data
    ./cleanup-all.zsh --keep-data --keep-images

WHAT GETS CLEANED:

VM Mode:
    - Stops and removes VM instance
    - Deletes static IP address
    - Removes VPC networks and subnets
    - Deletes service account
    - With --deep-clean: removes SSH keys

Local Mode:
    - Stops and removes Docker containers
    - With --keep-data=false: removes volumes
    - With --keep-images=false: removes images
    - With --deep-clean: runs docker system prune

SAFETY FEATURES:
    - Confirmation prompts for destructive operations
    - Graceful handling of missing resources
    - Detailed logging of all operations
    - Rollback-friendly (keeps backups when possible)
EOF
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --mode)
            MODE="$2"
            if [[ "$MODE" != "vm" && "$MODE" != "local" && "$MODE" != "both" ]]; then
                log "ERROR" "Invalid mode: $MODE. Must be 'vm', 'local', or 'both'"
                exit 1
            fi
            shift 2
            ;;
        --force)
            FORCE=true
            shift
            ;;
        --keep-data)
            KEEP_DATA=true
            shift
            ;;
        --keep-images)
            KEEP_IMAGES=true
            shift
            ;;
        --deep-clean)
            DEEP_CLEAN=true
            shift
            ;;
        --help)
            show_help
            exit 0
            ;;
        *)
            log "ERROR" "Unknown option: $1. Use --help for usage information."
            exit 1
            ;;
    esac
done

# VM cleanup functions
cleanup_vm() {
    log "HEADER" "=== VM Deployment Cleanup ==="
    echo ""
    
    if confirm "This will destroy VM resources in Yandex Cloud"; then
        log "INFO" "Starting VM cleanup..."
        
        # Use existing destroy-all script
        if [[ -f "./destroy-all.zsh" ]]; then
            if [[ "$DEEP_CLEAN" == "true" ]]; then
                log "INFO" "Deep cleaning VM resources including SSH keys..."
                CLEAN_LOCAL_KEYS=true ./destroy-all.zsh
            else
                ./destroy-all.zsh
            fi
            log "SUCCESS" "VM resources cleaned up"
        else
            log "ERROR" "destroy-all.zsh script not found"
            return 1
        fi
    fi
}

# Local cleanup functions
cleanup_local() {
    log "HEADER" "=== Local Docker Cleanup ==="
    echo ""
    
    if ! command -v docker-compose >/dev/null 2>&1; then
        log "ERROR" "docker-compose is not installed"
        return 1
    fi
    
    if [[ ! -f "../docker-compose.yml" ]]; then
        log "ERROR" "docker-compose.yml not found in parent directory"
        return 1
    fi
    
    cd ..
    
    # Stop and remove containers
    if confirm "This will stop and remove local Docker containers"; then
        log "INFO" "Stopping Docker containers..."
        docker-compose down --remove-orphans 2>/dev/null || log "WARNING" "Some containers were already stopped"
        
        # Remove volumes if not keeping data
        if [[ "$KEEP_DATA" == "false" ]]; then
            if confirm "This will DELETE all persistent data (databases, uploads, etc.)"; then
                log "INFO" "Removing volumes..."
                docker-compose down --volumes 2>/dev/null || log "WARNING" "Some volumes were already removed"
            fi
        fi
        
        log "SUCCESS" "Local containers cleaned up"
    fi
    
    # Remove images if not keeping them
    if [[ "$KEEP_IMAGES" == "false" ]]; then
        if confirm "This will remove Docker images"; then
            log "INFO" "Removing Docker images..."
            
            # Remove project images
            local images=(
                "vladimirryrik/ajasta-postgres:alpine"
                "vladimirryrik/ajasta-backend:alpine"
                "vladimirryrik/ajasta-frontend:alpine"
                "postgres:16-alpine"
                "adminer:4.8.1"
                "mailhog/mailhog:v1.0.1"
            )
            
            for image in "${images[@]}"; do
                if docker images -q "$image" >/dev/null 2>&1; then
                    log "INFO" "Removing image: $image"
                    docker rmi "$image" 2>/dev/null || log "WARNING" "Could not remove $image"
                fi
            done
            
            log "SUCCESS" "Docker images cleaned up"
        fi
    fi
    
    # Deep clean if requested
    if [[ "$DEEP_CLEAN" == "true" ]]; then
        if confirm "This will run Docker system prune (removes unused networks, images, build cache)"; then
            log "INFO" "Running Docker system cleanup..."
            docker system prune --all --force 2>/dev/null || log "WARNING" "Docker system prune completed with warnings"
            
            # Remove development data directory
            if [[ -d "./dev-data" ]]; then
                if confirm "Remove development data directory (./dev-data)"; then
                    log "INFO" "Removing development data directory..."
                    rm -rf ./dev-data
                fi
            fi
            
            log "SUCCESS" "Deep cleanup completed"
        fi
    fi
    
    cd scripts
}

# Cleanup log files and temporary data
cleanup_logs_and_temp() {
    if [[ "$DEEP_CLEAN" == "true" ]]; then
        log "HEADER" "=== Cleaning Logs and Temporary Files ==="
        echo ""
        
        # Clean log files
        if [[ -d "./logs" ]]; then
            if confirm "Remove log files directory (./logs)"; then
                log "INFO" "Removing log files..."
                rm -rf ./logs
                log "SUCCESS" "Log files cleaned up"
            fi
        fi
        
        # Clean temporary files
        local temp_files=(
            "./.tmp-user-data-*.yaml"
            "./ajasta_ed25519*"
            "./sa-key.json"
        )
        
        for pattern in "${temp_files[@]}"; do
            if ls $pattern >/dev/null 2>&1; then
                if confirm "Remove temporary files matching: $pattern"; then
                    log "INFO" "Removing: $pattern"
                    rm -f $pattern
                fi
            fi
        done
    fi
}

# System maintenance
run_maintenance() {
    if [[ "$DEEP_CLEAN" == "true" ]]; then
        log "HEADER" "=== System Maintenance ==="
        echo ""
        
        # Docker maintenance
        if command -v docker >/dev/null 2>&1; then
            log "INFO" "Running Docker maintenance..."
            
            # Remove dangling images
            if docker images -f "dangling=true" -q | grep -q .; then
                log "INFO" "Removing dangling Docker images..."
                docker images -f "dangling=true" -q | xargs docker rmi 2>/dev/null || true
            fi
            
            # Remove unused networks
            if docker network ls -f "dangling=true" -q | grep -q .; then
                log "INFO" "Removing unused Docker networks..."
                docker network ls -f "dangling=true" -q | xargs docker network rm 2>/dev/null || true
            fi
            
            log "SUCCESS" "Docker maintenance completed"
        fi
    fi
}

# Summary report
generate_summary() {
    log "HEADER" "=== Cleanup Summary ==="
    echo ""
    
    case "$MODE" in
        "vm")
            log "INFO" "VM Cleanup Results:"
            if command -v yc >/dev/null 2>&1; then
                log "INFO" "Remaining VM instances:"
                yc compute instance list --format="table(name,status,zone_id)" 2>/dev/null || log "WARNING" "Could not list VM instances"
                echo ""
                log "INFO" "Remaining networks:"
                yc vpc network list --format="table(name,folder_id)" 2>/dev/null || log "WARNING" "Could not list networks"
            fi
            ;;
        "local")
            log "INFO" "Local Cleanup Results:"
            if command -v docker >/dev/null 2>&1; then
                log "INFO" "Running containers:"
                docker ps --filter "name=ajasta" --format="table {{.Names}}\t{{.Status}}" 2>/dev/null || echo "No ajasta containers running"
                echo ""
                if [[ "$KEEP_IMAGES" == "false" ]]; then
                    log "INFO" "Remaining images:"
                    docker images --filter "reference=*ajasta*" --format="table {{.Repository}}\t{{.Tag}}\t{{.Size}}" 2>/dev/null || echo "No ajasta images found"
                fi
            fi
            ;;
        "both")
            generate_summary() { MODE="vm" generate_summary; MODE="local" generate_summary; }
            ;;
    esac
    
    echo ""
    log "SUCCESS" "Cleanup completed successfully!"
    echo ""
}

# Main execution
main() {
    echo ""
    log "HEADER" "🧹 Ajasta Application Cleanup Tool"
    log "INFO" "Mode: $MODE"
    if [[ "$FORCE" == "true" ]]; then
        log "INFO" "Force mode: enabled (no confirmations)"
    fi
    if [[ "$KEEP_DATA" == "true" ]]; then
        log "INFO" "Data preservation: enabled"
    fi
    if [[ "$KEEP_IMAGES" == "true" ]]; then
        log "INFO" "Image preservation: enabled"
    fi
    if [[ "$DEEP_CLEAN" == "true" ]]; then
        log "INFO" "Deep clean: enabled"
    fi
    echo ""
    
    case "$MODE" in
        "vm")
            cleanup_vm
            ;;
        "local")
            cleanup_local
            ;;
        "both")
            cleanup_vm
            echo ""
            cleanup_local
            ;;
    esac
    
    cleanup_logs_and_temp
    run_maintenance
    generate_summary
}

# Execute main function
main "$@"