#!/usr/bin/env zsh
# Log aggregation tool for Ajasta application
# Collects and displays logs from both VM and local deployments
#
# Usage:
#   ./logs-all.zsh [OPTIONS]
#
# Options:
#   --mode vm|local|both     Show logs from specific deployment (default: both)
#   --service SERVICE        Show logs from specific service (postgres|backend|frontend|all)
#   --follow                 Follow log output (tail -f behavior)
#   --lines N               Number of lines to show (default: 50)
#   --since DURATION        Show logs since duration (e.g., 1h, 30m, 2d)
#   --save                  Save logs to files
#   --help                  Show this help message

set -euo pipefail
SCRIPT_DIR=$(cd -- "${0:A:h}" && pwd)
cd "$SCRIPT_DIR"

# Default configuration
MODE="both"
SERVICE="all"
FOLLOW=false
LINES=50
SINCE=""
SAVE_LOGS=false

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

# Help function
show_help() {
    cat << EOF
📋 Ajasta Application Log Aggregator

This script collects and displays logs from Ajasta application services across different deployments.

USAGE:
    ./logs-all.zsh [OPTIONS]

OPTIONS:
    --mode vm|local|both      Show logs from specific deployment (default: both)
                             vm    - Show VM deployment logs
                             local - Show local Docker Compose logs  
                             both  - Show logs from both deployments

    --service SERVICE        Show logs from specific service (default: all)
                            postgres  - PostgreSQL database logs
                            backend   - Spring Boot API logs
                            frontend  - React/Nginx logs
                            all       - All service logs

    --follow                 Follow log output in real-time (like tail -f)
                            Note: Only works with single deployment mode

    --lines N               Number of lines to show (default: 50)

    --since DURATION        Show logs since duration
                           Examples: 1h, 30m, 2d, 2023-01-01T10:00:00

    --save                  Save logs to timestamped files in ./logs/

    --help                  Show this help message

EXAMPLES:
    # Show last 50 lines from all services
    ./logs-all.zsh

    # Follow backend logs from VM
    ./logs-all.zsh --mode vm --service backend --follow

    # Show last 100 lines from local deployment
    ./logs-all.zsh --mode local --lines 100

    # Show logs since 1 hour ago and save to files
    ./logs-all.zsh --since 1h --save

    # Follow all logs from local development
    ./logs-all.zsh --mode local --follow

OUTPUT:
    Logs are displayed with service names and timestamps when available.
    When --save is used, logs are saved to ./logs/ directory with timestamps.
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
        --service)
            SERVICE="$2"
            if [[ "$SERVICE" != "postgres" && "$SERVICE" != "backend" && "$SERVICE" != "frontend" && "$SERVICE" != "all" ]]; then
                log "ERROR" "Invalid service: $SERVICE. Must be 'postgres', 'backend', 'frontend', or 'all'"
                exit 1
            fi
            shift 2
            ;;
        --follow)
            FOLLOW=true
            shift
            ;;
        --lines)
            LINES="$2"
            if ! [[ "$LINES" =~ ^[0-9]+$ ]]; then
                log "ERROR" "Lines must be a number"
                exit 1
            fi
            shift 2
            ;;
        --since)
            SINCE="$2"
            shift 2
            ;;
        --save)
            SAVE_LOGS=true
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

# Validate follow mode
if [[ "$FOLLOW" == "true" && "$MODE" == "both" ]]; then
    log "ERROR" "Follow mode (--follow) cannot be used with --mode both. Choose vm or local."
    exit 1
fi

# Create logs directory if saving
if [[ "$SAVE_LOGS" == "true" ]]; then
    mkdir -p ./logs
    TIMESTAMP=$(date '+%Y%m%d_%H%M%S')
fi

# Get services to process
get_services() {
    case "$SERVICE" in
        "postgres") echo "ajasta-postgres" ;;
        "backend") echo "ajasta-backend" ;;
        "frontend") echo "ajasta-frontend" ;;
        "all") echo "ajasta-postgres ajasta-backend ajasta-frontend" ;;
    esac
}

# Show VM logs
show_vm_logs() {
    log "HEADER" "=== VM Deployment Logs ==="
    echo ""
    
    if ! ./ssh-ajasta.zsh "echo 'VM accessible'" >/dev/null 2>&1; then
        log "ERROR" "VM is not accessible via SSH"
        return 1
    fi
    
    local services=$(get_services)
    local docker_cmd="docker logs"
    
    # Add options to docker logs command
    if [[ -n "$SINCE" ]]; then
        docker_cmd="$docker_cmd --since $SINCE"
    fi
    
    if [[ "$FOLLOW" == "true" ]]; then
        docker_cmd="$docker_cmd --follow"
    else
        docker_cmd="$docker_cmd --tail $LINES"
    fi
    
    for service in $services; do
        log "INFO" "Fetching logs for $service on VM..."
        echo ""
        echo "=== $service logs ==="
        
        if [[ "$SAVE_LOGS" == "true" ]]; then
            local log_file="./logs/vm_${service}_${TIMESTAMP}.log"
            log "INFO" "Saving to $log_file"
            ./ssh-ajasta.zsh "$docker_cmd $service" > "$log_file" 2>&1 || log "WARNING" "Could not fetch logs for $service"
            echo "Logs saved to $log_file"
        else
            ./ssh-ajasta.zsh "$docker_cmd $service" 2>&1 || log "WARNING" "Could not fetch logs for $service"
        fi
        
        echo ""
    done
}

# Show local logs
show_local_logs() {
    log "HEADER" "=== Local Docker Compose Logs ==="
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
    
    local services=$(get_services)
    local compose_cmd="docker-compose logs"
    
    # Add options to docker-compose logs command
    if [[ -n "$SINCE" ]]; then
        compose_cmd="$compose_cmd --since $SINCE"
    fi
    
    if [[ "$FOLLOW" == "true" ]]; then
        compose_cmd="$compose_cmd --follow"
    else
        compose_cmd="$compose_cmd --tail=$LINES"
    fi
    
    if [[ "$SERVICE" == "all" ]]; then
        # Show all services
        log "INFO" "Fetching logs for all services..."
        echo ""
        
        if [[ "$SAVE_LOGS" == "true" ]]; then
            local log_file="../scripts/logs/local_all_services_${TIMESTAMP}.log"
            log "INFO" "Saving to $log_file"
            $compose_cmd > "$log_file" 2>&1 || log "WARNING" "Could not fetch logs"
            echo "Logs saved to $log_file"
        else
            $compose_cmd 2>&1 || log "WARNING" "Could not fetch logs"
        fi
    else
        # Show specific service
        local service_name=""
        case "$SERVICE" in
            "postgres") service_name="ajasta-postgres" ;;
            "backend") service_name="ajasta-backend" ;;
            "frontend") service_name="ajasta-frontend" ;;
        esac
        
        log "INFO" "Fetching logs for $service_name..."
        echo ""
        echo "=== $service_name logs ==="
        
        if [[ "$SAVE_LOGS" == "true" ]]; then
            local log_file="../scripts/logs/local_${service_name}_${TIMESTAMP}.log"
            log "INFO" "Saving to $log_file"
            $compose_cmd $service_name > "$log_file" 2>&1 || log "WARNING" "Could not fetch logs for $service_name"
            echo "Logs saved to $log_file"
        else
            $compose_cmd $service_name 2>&1 || log "WARNING" "Could not fetch logs for $service_name"
        fi
    fi
    
    cd scripts
}

# Main execution
main() {
    echo ""
    log "HEADER" "📋 Ajasta Application Log Aggregator"
    log "INFO" "Mode: $MODE | Service: $SERVICE | Lines: $LINES"
    if [[ -n "$SINCE" ]]; then
        log "INFO" "Since: $SINCE"
    fi
    if [[ "$FOLLOW" == "true" ]]; then
        log "INFO" "Following logs (Ctrl+C to stop)"
    fi
    if [[ "$SAVE_LOGS" == "true" ]]; then
        log "INFO" "Saving logs to ./logs/ directory"
    fi
    echo ""
    
    case "$MODE" in
        "vm")
            show_vm_logs
            ;;
        "local")
            show_local_logs
            ;;
        "both")
            if [[ "$FOLLOW" == "true" ]]; then
                log "ERROR" "Cannot follow logs from both deployments simultaneously"
                exit 1
            fi
            show_vm_logs
            echo ""
            show_local_logs
            ;;
    esac
    
    echo ""
    log "SUCCESS" "Log aggregation completed"
    echo ""
}

# Execute main function
main "$@"