#!/bin/bash

# Terraform setup script for GitLab CI/CD
# This script handles all the complex setup that was causing nesting issues

set -e

echo "🔧 Starting Terraform setup..."

# System detection and package installation
echo "🖥  Detecting operating system..."
if [[ "$OSTYPE" == "linux-gnu"* ]]; then
    echo "✅ Linux detected"
    if command -v apt-get >/dev/null 2>&1; then
        sudo apt-get update -qq
        sudo apt-get install -y curl unzip jq openssh-client
    elif command -v yum >/dev/null 2>&1; then
        sudo yum install -y curl unzip jq openssh-clients
    elif command -v apk >/dev/null 2>&1; then
        apk add --no-cache curl unzip jq openssh-client
    else
        echo "❌ Unknown Linux distribution"
        exit 1
    fi
elif [[ "$OSTYPE" == "darwin"* ]]; then
    echo "✅ macOS detected"
    if ! command -v jq >/dev/null 2>&1; then
        echo "📦 Installing jq..."
        brew install jq || echo "⚠️  Please install jq manually: brew install jq"
    fi
    if ! command -v terraform >/dev/null 2>&1; then
        echo "📦 Installing Terraform..."
        brew install terraform || echo "⚠️  Please install Terraform manually: brew install terraform"
    fi
else
    echo "❌ Unknown OS: $OSTYPE"
    exit 1
fi

# Install Terraform if not present
if ! command -v terraform >/dev/null 2>&1; then
    echo "📦 Installing Terraform..."
    TERRAFORM_VERSION="1.8.0"
    if [[ "$OSTYPE" == "linux-gnu"* ]]; then
        wget https://releases.hashicorp.com/terraform/${TERRAFORM_VERSION}/terraform_${TERRAFORM_VERSION}_linux_amd64.zip
        unzip terraform_${TERRAFORM_VERSION}_linux_amd64.zip
        sudo mv terraform /usr/local/bin/
    elif [[ "$OSTYPE" == "darwin"* ]]; then
        wget https://releases.hashicorp.com/terraform/${TERRAFORM_VERSION}/terraform_${TERRAFORM_VERSION}_darwin_amd64.zip
        unzip terraform_${TERRAFORM_VERSION}_darwin_amd64.zip
        sudo mv terraform /usr/local/bin/
    fi
    rm -f terraform_${TERRAFORM_VERSION}_*.zip
fi

echo "🔑 Extracting SSH public key from private key..."
if [ -n "${SSH_PRIVATE_KEY:-}" ]; then
    echo "${SSH_PRIVATE_KEY}" | tr -d '\r' > /tmp/id_rsa
    chmod 600 /tmp/id_rsa
    if ssh-keygen -y -f /tmp/id_rsa > /tmp/id_rsa.pub 2>/dev/null; then
        TF_VAR_ssh_public_key="$(cat /tmp/id_rsa.pub)"
        echo "✅ SSH public key extracted successfully"
    else
        echo "❌ Failed to extract SSH public key"
        TF_VAR_ssh_public_key=""
    fi
    rm -f /tmp/id_rsa /tmp/id_rsa.pub
else
    echo "⚠️  No SSH private key provided"
    TF_VAR_ssh_public_key=""
fi

echo "📝 Creating terraform.tfvars from environment variables..."
cat > terraform.tfvars << 'EOF'
# Yandex Cloud configuration
yc_cloud_id  = "'"${TF_VAR_yc_cloud_id}"'"
yc_folder_id = "'"${TF_VAR_yc_folder_id}"'"
yc_zone      = "ru-central1-b"

# SSH configuration
ssh_username = "'"${TF_VAR_ssh_username}"'"
ssh_public_key = "'"${TF_VAR_ssh_public_key}"'"

# Networking
yc_network_name = "external-ajasta-network"
yc_subnet_name = "ajasta-external-segment"
yc_subnet_cidr = "172.16.17.0/28"
yc_internal_network_name = "internal-ajasta-network"
yc_internal_subnet_name = "ajasta-internal-segment"
yc_internal_subnet_cidr = "10.10.0.0/24"

# Static addresses
master_address_name = "ajasta-k8s-master-ip"
workers = [
  { vm_name = "k8s-worker-1", address_name = "ajasta-k8s-worker1-ip" },
  { vm_name = "k8s-worker-2", address_name = "ajasta-k8s-worker2-ip" },
  { vm_name = "k8s-worker-3", address_name = "ajasta-k8s-worker3-ip" },
]

# VM specifications
master_vm_name = "k8s-master"
master_vm_memory = 6
master_vm_cores = 2
master_vm_disk_size = 30
worker_vm_memory = 6
worker_vm_cores = 2
worker_vm_disk_size = 30

# cloud-init user-data
metadata_yaml = "../scripts/metadata.yaml"
EOF

echo "🔍 Debug: Terraform variables check"
echo "   yc_cloud_id: ${TF_VAR_yc_cloud_id:0:20}..."
echo "   yc_folder_id: ${TF_VAR_yc_folder_id:0:20}..."
echo "   ssh_username: ${TF_VAR_ssh_username}"
echo "   ssh_public_key: ${TF_VAR_ssh_public_key:0:30}..."
echo "   SSH_PRIVATE_KEY length: ${#SSH_PRIVATE_KEY}"
echo ""

echo "📄 terraform.tfvars created successfully:"
head -10 terraform.tfvars

echo "🚀 Terraform setup completed successfully!"