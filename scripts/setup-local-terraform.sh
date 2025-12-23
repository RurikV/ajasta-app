#!/bin/bash

# Local Terraform Setup Script
# This script sets up Terraform for local development

set -e

echo "🔧 Setting up Terraform for local development..."

# Navigate to terraform directory
cd terraform

# For local development, create a local backend file
echo "📝 Configuring local backend for development..."
if [ -f "backend.tf" ]; then
    # Backup the original backend file
    cp backend.tf backend.tf.backup
fi

# Create local backend configuration
cat > backend.tf << 'EOF'
# Terraform Backend Configuration for Local Development
terraform {
  backend "local" {
    path = "terraform.tfstate"
  }
}
EOF

echo "✅ Local backend configured"

# Check if terraform.tfvars exists
if [ ! -f "terraform.tfvars" ]; then
    echo "📄 Creating terraform.tfvars for local development..."
    # Run the main terraform setup script
    cd ..
    ./scripts/terraform-setup.sh
    cd terraform
fi

# Initialize Terraform
echo "🚀 Initializing Terraform..."
terraform init

echo "✅ Local Terraform setup completed!"
echo ""
echo "🎯 Next steps:"
echo "1. Review terraform.tfvars configuration"
echo "2. Run: terraform plan"
echo "3. Run: terraform apply"
echo ""
echo "📊 To check deployment status:"
echo "   terraform show"