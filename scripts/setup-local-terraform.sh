#!/bin/bash

# Local Terraform Setup Script
# This script sets up Terraform for local development

set -e

echo "🔧 Setting up Terraform for local development..."

# Navigate to terraform directory
cd terraform

# Enable local backend by uncommenting it
echo "📝 Configuring local backend..."
if [ -f "backend.tf" ]; then
    # Uncomment the local backend configuration
    sed -i '' 's/# terraform {/terraform {/' backend.tf
    sed -i '' 's/# }/}/' backend.tf
    sed -i '' 's/#   backend "local"/  backend "local"/' backend.tf
    sed -i '' 's/#     path = "terraform.tfstate"/    path = "terraform.tfstate"/' backend.tf

    echo "✅ Local backend configured"
else
    echo "⚠️  backend.tf file not found, creating..."
    cat > backend.tf << 'EOF'
terraform {
  backend "local" {
    path = "terraform.tfstate"
  }
}
EOF
fi

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