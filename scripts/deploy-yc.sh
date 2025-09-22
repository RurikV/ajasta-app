#!/bin/bash

# Yandex Cloud Deployment Script for Ajasta App
# Идемпотентный деплой в Yandex Cloud с проверками существования ресурсов

set -euo pipefail

# Configuration
ENVIRONMENT=${1:-production}
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Yandex Cloud configuration
YC_ZONE=${YC_ZONE:-ru-central1-a}
YC_INSTANCE_NAME=${YC_INSTANCE_NAME:-ajasta-app-vm}
YC_INSTANCE_TYPE=${YC_INSTANCE_TYPE:-standard-v3}
YC_CORES=${YC_CORES:-2}
YC_MEMORY=${YC_MEMORY:-4GB}
YC_DISK_SIZE=${YC_DISK_SIZE:-20GB}
YC_SUBNET_NAME="ajasta-subnet"
YC_NETWORK_NAME="ajasta-network"
YC_SECURITY_GROUP_NAME="ajasta-sg"
YC_SERVICE_ACCOUNT_NAME="ajasta-sa"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

log() {
    echo -e "${BLUE}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if Yandex Cloud CLI is installed and configured
check_yc_cli() {
    log "Checking Yandex Cloud CLI..."
    
    if ! command -v yc &> /dev/null; then
        log_error "Yandex Cloud CLI is not installed"
        exit 1
    fi
    
    if ! yc config list &> /dev/null; then
        log_error "Yandex Cloud CLI is not configured"
        exit 1
    fi
    
    log_success "Yandex Cloud CLI is ready"
}

# Create or get VPC network (idempotent)
ensure_network() {
    log "Ensuring VPC network exists..."
    
    local network_id
    network_id=$(yc vpc network list --format json | jq -r ".[] | select(.name == \"$YC_NETWORK_NAME\") | .id")
    
    if [[ -z "$network_id" || "$network_id" == "null" ]]; then
        log "Creating VPC network: $YC_NETWORK_NAME"
        network_id=$(yc vpc network create \
            --name "$YC_NETWORK_NAME" \
            --description "Network for Ajasta App" \
            --format json | jq -r '.id')
        log_success "Created network with ID: $network_id"
    else
        log_success "Network already exists with ID: $network_id"
    fi
    
    echo "$network_id"
}

# Create or get subnet (idempotent)
ensure_subnet() {
    local network_id=$1
    log "Ensuring subnet exists..."
    
    local subnet_id
    subnet_id=$(yc vpc subnet list --format json | jq -r ".[] | select(.name == \"$YC_SUBNET_NAME\") | .id")
    
    if [[ -z "$subnet_id" || "$subnet_id" == "null" ]]; then
        log "Creating subnet: $YC_SUBNET_NAME"
        subnet_id=$(yc vpc subnet create \
            --name "$YC_SUBNET_NAME" \
            --description "Subnet for Ajasta App" \
            --network-id "$network_id" \
            --zone "$YC_ZONE" \
            --range "10.0.0.0/24" \
            --format json | jq -r '.id')
        log_success "Created subnet with ID: $subnet_id"
    else
        log_success "Subnet already exists with ID: $subnet_id"
    fi
    
    echo "$subnet_id"
}

# Create or get security group (idempotent)
ensure_security_group() {
    local network_id=$1
    log "Ensuring security group exists..."
    
    local sg_id
    sg_id=$(yc vpc security-group list --format json | jq -r ".[] | select(.name == \"$YC_SECURITY_GROUP_NAME\") | .id")
    
    if [[ -z "$sg_id" || "$sg_id" == "null" ]]; then
        log "Creating security group: $YC_SECURITY_GROUP_NAME"
        sg_id=$(yc vpc security-group create \
            --name "$YC_SECURITY_GROUP_NAME" \
            --description "Security group for Ajasta App" \
            --network-id "$network_id" \
            --rule "direction=ingress,port=22,protocol=tcp,v4-cidrs=[0.0.0.0/0]" \
            --rule "direction=ingress,port=80,protocol=tcp,v4-cidrs=[0.0.0.0/0]" \
            --rule "direction=ingress,port=8090,protocol=tcp,v4-cidrs=[0.0.0.0/0]" \
            --rule "direction=ingress,port=5432,protocol=tcp,v4-cidrs=[10.0.0.0/24]" \
            --rule "direction=egress,protocol=any,v4-cidrs=[0.0.0.0/0]" \
            --format json | jq -r '.id')
        log_success "Created security group with ID: $sg_id"
    else
        log_success "Security group already exists with ID: $sg_id"
    fi
    
    echo "$sg_id"
}

# Create or get service account (idempotent)
ensure_service_account() {
    log "Ensuring service account exists..."
    
    local sa_id
    sa_id=$(yc iam service-account list --format json | jq -r ".[] | select(.name == \"$YC_SERVICE_ACCOUNT_NAME\") | .id")
    
    if [[ -z "$sa_id" || "$sa_id" == "null" ]]; then
        log "Creating service account: $YC_SERVICE_ACCOUNT_NAME"
        sa_id=$(yc iam service-account create \
            --name "$YC_SERVICE_ACCOUNT_NAME" \
            --description "Service account for Ajasta App" \
            --format json | jq -r '.id')
        
        # Assign necessary roles
        yc resource-manager folder add-access-binding \
            --id "$YC_FOLDER_ID" \
            --role "container-registry.images.puller" \
            --service-account-id "$sa_id"
            
        log_success "Created service account with ID: $sa_id"
    else
        log_success "Service account already exists with ID: $sa_id"
    fi
    
    echo "$sa_id"
}

# Create or update VM instance (idempotent)
ensure_vm_instance() {
    local subnet_id=$1
    local sg_id=$2
    local sa_id=$3
    
    log "Ensuring VM instance exists..."
    
    local instance_id
    instance_id=$(yc compute instance list --format json | jq -r ".[] | select(.name == \"$YC_INSTANCE_NAME\") | .id")
    
    if [[ -z "$instance_id" || "$instance_id" == "null" ]]; then
        log "Creating VM instance: $YC_INSTANCE_NAME"
        
        # Create SSH key if not exists
        if [[ ! -f ~/.ssh/yc_key ]]; then
            ssh-keygen -t rsa -b 2048 -f ~/.ssh/yc_key -N "" -C "yc-deploy-key"
        fi
        
        local ssh_key
        ssh_key=$(cat ~/.ssh/yc_key.pub)
        
        instance_id=$(yc compute instance create \
            --name "$YC_INSTANCE_NAME" \
            --description "VM for Ajasta App ($ENVIRONMENT)" \
            --zone "$YC_ZONE" \
            --network-interface "subnet-id=$subnet_id,nat-ip-version=ipv4,security-group-ids=$sg_id" \
            --create-boot-disk "image-folder-id=standard-images,image-family=ubuntu-2204-lts,size=$YC_DISK_SIZE,type=network-hdd" \
            --cores "$YC_CORES" \
            --memory "$YC_MEMORY" \
            --ssh-key "$ssh_key" \
            --service-account-id "$sa_id" \
            --metadata-from-file user-data="$SCRIPT_DIR/cloud-init.yml" \
            --format json | jq -r '.id')
            
        log_success "Created VM instance with ID: $instance_id"
        
        # Wait for instance to be ready
        log "Waiting for instance to be ready..."
        while [[ $(yc compute instance get "$instance_id" --format json | jq -r '.status') != "RUNNING" ]]; do
            sleep 10
            log "Instance is starting..."
        done
        
        sleep 30 # Additional wait for cloud-init to complete
    else
        log_success "VM instance already exists with ID: $instance_id"
    fi
    
    # Get external IP
    local external_ip
    external_ip=$(yc compute instance get "$instance_id" --format json | jq -r '.network_interfaces[0].primary_v4_address.one_to_one_nat.address')
    log_success "VM external IP: $external_ip"
    
    echo "$instance_id $external_ip"
}

# Deploy application to VM
deploy_application() {
    local external_ip=$1
    log "Deploying application to VM: $external_ip"
    
    # Wait for SSH to be available
    log "Waiting for SSH connection..."
    local max_attempts=30
    local attempt=1
    
    while ! ssh -o StrictHostKeyChecking=no -o ConnectTimeout=5 -i ~/.ssh/yc_key "ubuntu@$external_ip" "echo 'SSH connection successful'" &>/dev/null; do
        if [[ $attempt -ge $max_attempts ]]; then
            log_error "Failed to establish SSH connection after $max_attempts attempts"
            exit 1
        fi
        log "Attempt $attempt/$max_attempts: SSH not ready yet, waiting..."
        sleep 10
        ((attempt++))
    done
    
    log_success "SSH connection established"
    
    # Copy deployment files
    log "Copying deployment files..."
    scp -o StrictHostKeyChecking=no -i ~/.ssh/yc_key -r "$PROJECT_ROOT/deploy/" "ubuntu@$external_ip:~/deploy/"
    
    # Execute deployment on VM
    log "Executing deployment on VM..."
    ssh -o StrictHostKeyChecking=no -i ~/.ssh/yc_key "ubuntu@$external_ip" << EOF
set -euo pipefail

# Load environment variables
cd ~/deploy
source .env

# Login to GitLab registry
echo "$CI_REGISTRY_PASSWORD" | sudo docker login -u "$CI_REGISTRY_USER" --password-stdin "$CI_REGISTRY"

# Update docker-compose with new image tags
export DOCKERHUB_USER=""
export BACKEND_IMAGE="\$BACKEND_IMAGE"
export FRONTEND_IMAGE="\$FRONTEND_IMAGE"

# Stop existing containers (idempotent)
sudo docker-compose -f docker-compose.yml down --remove-orphans || true

# Pull new images
sudo docker-compose -f docker-compose.yml pull

# Start services
sudo docker-compose -f docker-compose.yml up -d

# Wait for services to be healthy
echo "Waiting for services to be ready..."
timeout 300s bash -c 'until sudo docker-compose -f docker-compose.yml ps | grep -q "healthy"; do sleep 5; done' || {
    echo "Services failed to become healthy within timeout"
    sudo docker-compose -f docker-compose.yml logs
    exit 1
}

echo "Deployment completed successfully!"
EOF
    
    log_success "Application deployed successfully"
    log_success "Frontend URL: http://$external_ip"
    log_success "Backend API URL: http://$external_ip:8090"
}

# Main deployment function
main() {
    log "Starting Yandex Cloud deployment for environment: $ENVIRONMENT"
    
    check_yc_cli
    
    # Ensure required environment variables are set
    if [[ -z "${YC_TOKEN:-}" ]] || [[ -z "${YC_CLOUD_ID:-}" ]] || [[ -z "${YC_FOLDER_ID:-}" ]]; then
        log_error "Required environment variables are not set:"
        log_error "YC_TOKEN, YC_CLOUD_ID, YC_FOLDER_ID"
        exit 1
    fi
    
    # Create infrastructure (idempotent)
    local network_id
    network_id=$(ensure_network)
    
    local subnet_id
    subnet_id=$(ensure_subnet "$network_id")
    
    local sg_id
    sg_id=$(ensure_security_group "$network_id")
    
    local sa_id
    sa_id=$(ensure_service_account)
    
    # Create or update VM instance
    local vm_info
    vm_info=$(ensure_vm_instance "$subnet_id" "$sg_id" "$sa_id")
    local instance_id external_ip
    instance_id=$(echo "$vm_info" | cut -d' ' -f1)
    external_ip=$(echo "$vm_info" | cut -d' ' -f2)
    
    # Deploy application
    deploy_application "$external_ip"
    
    log_success "Deployment completed successfully!"
    log_success "Instance ID: $instance_id"
    log_success "External IP: $external_ip"
    log_success "Frontend: http://$external_ip"
    log_success "Backend API: http://$external_ip:8090"
}

# Run main function
main "$@"