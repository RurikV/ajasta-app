#!/usr/bin/env zsh
# Universal status checker for Ajasta application
# Works with both VM and local Docker Compose deployments
#
# Usage:
#   ./status-all.zsh [OPTIONS]
#
# Options:
#   --mode vm|local|both     Check status for specific deployment (default: both)
#   --verbose               Show detailed information
#   --logs                  Show recent logs for each service
#   --health                Show health check status
#   --help                  Show this help message

set -euo pipefail
SCRIPT_DIR=$(cd -- "${0:A:h}" && pwd)
cd "$SCRIPT_DIR"

# Default configuration
MODE="both"
VERBOSE=false
SHOW_LOGS=false
SHOW_HEALTH=false

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Status indicators
STATUS_RUNNING="${GREEN}●${NC}"
STATUS_STOPPED="${RED}●${NC}"
STATUS_STARTING="${YELLOW}●${NC}"
STATUS_UNKNOWN="${PURPLE}●${NC}"

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
🔍 Ajasta Application Status Checker

This script checks the status of Ajasta application services across different deployment modes.

USAGE:
    ./status-all.zsh [OPTIONS]

OPTIONS:
    --mode vm|local|both    Check specific deployment mode (default: both)
                           vm    - Check VM deployment status
                           local - Check local Docker Compose status
                           both  - Check both deployments

    --verbose              Show detailed information including:
                          - Container resource usage
                          - Network information  
                          - Volume information

    --logs                 Show recent logs (last 20 lines) for each service

    --health               Show health check status and details

    --help                 Show this help message

EXAMPLES:
    # Check status of all deployments
    ./status-all.zsh

    # Check only VM deployment with logs
    ./status-all.zsh --mode vm --logs

    # Check local deployment with verbose output
    ./status-all.zsh --mode local --verbose

    # Full status check with health and logs
    ./status-all.zsh --verbose --logs --health

OUTPUT FORMAT:
    ● Green  - Service is running and healthy
    ● Yellow - Service is starting or unhealthy
    ● Red    - Service is stopped or failed
    ● Purple - Service status is unknown
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
        --verbose)
            VERBOSE=true
            shift
            ;;
        --logs)
            SHOW_LOGS=true
            shift
            ;;
        --health)
            SHOW_HEALTH=true
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

# Check VM deployment status
check_vm_status() {
    log "HEADER" "=== VM Deployment Status ==="
    echo ""
    
    # Check if VM is accessible
    if ./ssh-ajasta.zsh "echo 'VM accessible'" >/dev/null 2>&1; then
        log "SUCCESS" "VM is accessible via SSH"
        
        # Get VM IP
        local vm_ip
        vm_ip=$(./ssh-ajasta.zsh "curl -s http://ifconfig.me 2>/dev/null" || echo "UNKNOWN")
        log "INFO" "VM IP: $vm_ip"
        
        # Check Docker status on VM
        if ./ssh-ajasta.zsh "docker --version" >/dev/null 2>&1; then
            log "SUCCESS" "Docker is running on VM"
            
            # Get container status
            echo ""
            echo "Container Status:"
            ./ssh-ajasta.zsh "/opt/ajasta/status.sh" || log "WARNING" "Could not get container status"
            
            if [[ "$SHOW_HEALTH" == "true" ]]; then
                echo ""
                echo "Health Check Status:"
                ./ssh-ajasta.zsh "docker inspect ajasta-postgres ajasta-backend ajasta-frontend --format='{{.Name}}: {{.State.Health.Status}}' 2>/dev/null || echo 'Health checks not available'"
            fi
            
            if [[ "$VERBOSE" == "true" ]]; then
                echo ""
                echo "Resource Usage:"
                ./ssh-ajasta.zsh "docker stats --no-stream --format 'table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.NetIO}}' ajasta-postgres ajasta-backend ajasta-frontend 2>/dev/null || echo 'Stats not available'"
                
                echo ""
                echo "Network Information:"
                ./ssh-ajasta.zsh "docker network ls | grep ajasta || echo 'No ajasta networks found'"
                
                echo ""
                echo "Volume Information:"
                ./ssh-ajasta.zsh "docker volume ls | grep ajasta || echo 'No ajasta volumes found'"
            fi
            
            if [[ "$SHOW_LOGS" == "true" ]]; then
                echo ""
                echo "Recent Logs:"
                for container in ajasta-postgres ajasta-backend ajasta-frontend; do
                    echo ""
                    echo "--- $container logs (last 10 lines) ---"
                    ./ssh-ajasta.zsh "docker logs --tail 10 $container 2>&1 || echo 'No logs available for $container'"
                done
            fi
            
            # Test endpoints if VM IP is available
            if [[ "$vm_ip" != "UNKNOWN" ]]; then
                echo ""
                echo "Endpoint Tests:"
                
                # Test frontend
                if ./ssh-ajasta.zsh "curl -s -o /dev/null -w '%{http_code}' http://localhost/ | grep -q '200'"; then
                    echo -e "  Frontend (port 80):  $STATUS_RUNNING http://$vm_ip"
                else
                    echo -e "  Frontend (port 80):  $STATUS_STOPPED http://$vm_ip"
                fi
                
                # Test backend
                if ./ssh-ajasta.zsh "curl -s -o /dev/null -w '%{http_code}' http://localhost:8090/ | grep -qE '(200|401)'"; then
                    echo -e "  Backend (port 8090): $STATUS_RUNNING http://$vm_ip:8090"
                else
                    echo -e "  Backend (port 8090): $STATUS_STOPPED http://$vm_ip:8090"
                fi
                
                # Test database
                if ./ssh-ajasta.zsh "docker exec ajasta-postgres pg_isready -U admin -d ajastadb >/dev/null 2>&1"; then
                    echo -e "  Database (port 15432): $STATUS_RUNNING $vm_ip:15432"
                else
                    echo -e "  Database (port 15432): $STATUS_STOPPED $vm_ip:15432"
                fi
            fi
            
        else
            log "ERROR" "Docker is not available on VM"
        fi
        
    else
        log "ERROR" "VM is not accessible via SSH"
        log "INFO" "Check VM status with: yc compute instance get --name ajasta-host"
        log "INFO" "Try: ./ssh-ajasta.zsh 'echo test'"
    fi
}

# Check local deployment status
check_local_status() {
    log "HEADER" "=== Local Docker Compose Status ==="
    echo ""
    
    # Check if docker-compose is available
    if ! command -v docker-compose >/dev/null 2>&1; then
        log "ERROR" "docker-compose is not installed"
        return 1
    fi
    
    # Check if docker-compose.yml exists
    if [[ ! -f "../docker-compose.yml" ]]; then
        log "ERROR" "docker-compose.yml not found in parent directory"
        return 1
    fi
    
    cd ..
    
    # Get service status
    echo "Service Status:"
    if docker-compose ps 2>/dev/null; then
        echo ""
        
        # Check individual service health
        echo "Service Health:"
        for service in ajasta-postgres ajasta-backend ajasta-frontend; do
            local status
            status=$(docker-compose ps -q $service | xargs docker inspect --format='{{.State.Status}}' 2>/dev/null || echo "not_found")
            case "$status" in
                "running") echo -e "  $service: $STATUS_RUNNING Running" ;;
                "exited"|"dead") echo -e "  $service: $STATUS_STOPPED Stopped" ;;
                "restarting") echo -e "  $service: $STATUS_STARTING Restarting" ;;
                "not_found") echo -e "  $service: $STATUS_UNKNOWN Not Found" ;;
                *) echo -e "  $service: $STATUS_UNKNOWN $status" ;;
            esac
        done
        
        if [[ "$SHOW_HEALTH" == "true" ]]; then
            echo ""
            echo "Health Check Details:"
            docker-compose ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}" 2>/dev/null || echo "Health details not available"
        fi
        
        if [[ "$VERBOSE" == "true" ]]; then
            echo ""
            echo "Resource Usage:"
            docker stats --no-stream --format 'table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.NetIO}}' $(docker-compose ps -q) 2>/dev/null || echo "Stats not available"
            
            echo ""
            echo "Network Information:"
            docker network ls | grep ajasta || echo "No ajasta networks found"
            
            echo ""
            echo "Volume Information:"
            docker volume ls | grep -E "(ajasta|$(basename $(pwd)))" || echo "No project volumes found"
        fi
        
        if [[ "$SHOW_LOGS" == "true" ]]; then
            echo ""
            echo "Recent Logs:"
            docker-compose logs --tail=10 || log "WARNING" "Could not retrieve logs"
        fi
        
        # Test local endpoints
        echo ""
        echo "Endpoint Tests:"
        
        # Test frontend
        if curl -s -o /dev/null -w '%{http_code}' http://localhost:3000 | grep -q '200' 2>/dev/null; then
            echo -e "  Frontend:  $STATUS_RUNNING http://localhost:3000"
        else
            echo -e "  Frontend:  $STATUS_STOPPED http://localhost:3000"
        fi
        
        # Test backend
        if curl -s -o /dev/null -w '%{http_code}' http://localhost:8090 | grep -qE '(200|401)' 2>/dev/null; then
            echo -e "  Backend:   $STATUS_RUNNING http://localhost:8090"
        else
            echo -e "  Backend:   $STATUS_STOPPED http://localhost:8090"
        fi
        
        # Test database (check if port is listening)
        if nc -z localhost 15432 2>/dev/null; then
            echo -e "  Database:  $STATUS_RUNNING localhost:15432"
        else
            echo -e "  Database:  $STATUS_STOPPED localhost:15432"
        fi
        
        # Additional development services
        if docker-compose ps adminer >/dev/null 2>&1; then
            if curl -s -o /dev/null -w '%{http_code}' http://localhost:8080 | grep -q '200' 2>/dev/null; then
                echo -e "  Adminer:   $STATUS_RUNNING http://localhost:8080 (DB Admin)"
            else
                echo -e "  Adminer:   $STATUS_STOPPED http://localhost:8080"
            fi
        fi
        
        if docker-compose ps mailhog >/dev/null 2>&1; then
            if curl -s -o /dev/null -w '%{http_code}' http://localhost:8025 | grep -q '200' 2>/dev/null; then
                echo -e "  Mailhog:   $STATUS_RUNNING http://localhost:8025 (Email Testing)"
            else
                echo -e "  Mailhog:   $STATUS_STOPPED http://localhost:8025"
            fi
        fi
        
    else
        log "WARNING" "No services are running or docker-compose.yml has issues"
        log "INFO" "Start services with: docker-compose up -d"
    fi
    
    cd scripts
}

# Main execution
main() {
    echo ""
    log "HEADER" "🔍 Ajasta Application Status Check"
    echo ""
    
    case "$MODE" in
        "vm")
            check_vm_status
            ;;
        "local")
            check_local_status
            ;;
        "both")
            check_vm_status
            echo ""
            check_local_status
            ;;
    esac
    
    echo ""
    log "SUCCESS" "Status check completed"
    echo ""
}

# Execute main function
main "$@"