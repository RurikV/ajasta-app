#!/bin/bash

# Complete Terraform Destroy Script
# This script properly destroys all Terraform-managed resources and state

set -e

echo "🔥 Starting complete Terraform destruction..."

# Configuration
TERRAFORM_DIR="${TERRAFORM_DIR:-terraform}"
FORCE_DESTROY="${FORCE_DESTROY:-false}"

# Function to check if terraform directory exists
check_terraform_dir() {
    if [ ! -d "$TERRAFORM_DIR" ]; then
        echo "❌ Terraform directory not found: $TERRAFORM_DIR"
        exit 1
    fi

    cd "$TERRAFORM_DIR"
    echo "📁 Working directory: $(pwd)"
}

# Function to initialize Terraform (if needed)
init_terraform() {
    # Check for Yandex Cloud authentication
    if [ -z "${YC_TOKEN}" ] && [ -z "${YC_CLOUD_ID}" ] && [ -z "${YC_FOLDER_ID}" ]; then
        echo "❌ ERROR: Yandex Cloud authentication not configured!"
        echo ""
        echo "Please set the following environment variables:"
        echo "  export YC_TOKEN=\"your-oauth-token\""
        echo "  export YC_CLOUD_ID=\"your-cloud-id\""
        echo "  export YC_FOLDER_ID=\"your-folder-id\""
        echo ""
        echo "You can find these values by running:"
        echo "  yc config list"
        echo ""
        echo "Or get a new OAuth token at:"
        echo "  https://oauth.yandexcloud.com/authorize?response_type=token"
        exit 1
    fi

    echo "🔧 Initializing Terraform with HTTP backend..."

    # Initialize with HTTP backend (may fail if no GitLab credentials, but that's OK)
    # The backend configuration is in backend.tf
    terraform init -input=false || true
}

# Function to check Terraform state
check_terraform_state() {
    if [ ! -f "terraform.tfstate" ]; then
        echo "⚠️  No terraform.tfstate file found"
        echo "This might be a fresh workspace or state is stored remotely"
        return 1
    fi

    echo "📊 Terraform state analysis:"
    terraform show | grep -E "(resource|id)" | head -10 || true
}

# Function to force destroy all resources
force_destroy() {
    echo "💥 Force destroying all Terraform resources..."

    # Create a destroy plan (ignore lock release errors)
    echo "📋 Creating destroy plan..."
    terraform plan -destroy -out=destroy.tfplan -input=false || true

    # Check if plan was created
    if [ ! -f "destroy.tfplan" ]; then
        echo "⚠️  Destroy plan not created (may have no resources to destroy)"
        return 0
    fi

    # Apply the destroy plan (ignore lock release errors)
    echo "🔥 Applying destroy plan..."
    terraform apply -input=false -auto-approve destroy.tfplan || true

    echo "✅ All Terraform resources destroyed!"
}

# Function to clean up remaining resources manually
cleanup_remaining() {
    echo "🧹 Checking for remaining resources to clean up..."

    # Use the cleanup script if available
    if [ -f "../scripts/cleanup-yandex-resources.sh" ]; then
        echo "🛠️  Using Yandex Cloud cleanup script..."
        ../scripts/cleanup-yandex-resources.sh cleanup
    else
        echo "⚠️  Cleanup script not found"
    fi
}

# Function to remove Terraform state files
remove_state_files() {
    echo "🗑️  Removing Terraform state files..."

    # Backup state before removal
    if [ -f "terraform.tfstate" ]; then
        cp terraform.tfstate "terraform.tfstate.backup.$(date +%Y%m%d_%H%M%S)"
        echo "💾 State backed up"
    fi

    # Remove state files
    rm -f terraform.tfstate
    rm -f terraform.tfstate.backup
    rm -f .terraform.lock.hcl
    rm -f *.tfplan

    # Remove .terraform directory
    rm -rf .terraform

    echo "✅ Terraform state files removed"
}

# Function to remove generated tfvars
remove_tfvars() {
    echo "🗑️  Removing generated terraform.tfvars..."
    rm -f terraform.tfvars
    echo "✅ terraform.tfvars removed"
}

# Main execution
main() {
    echo "🎯 Complete Terraform Destruction Process"
    echo "========================================"

    check_terraform_dir

    # Check if we want to force destroy or ask for confirmation
    if [ "$FORCE_DESTROY" = "true" ]; then
        echo "⚡ Force destroy mode enabled"
    else
        echo "⚠️  WARNING: This will destroy ALL Terraform-managed resources!"
        echo "   This includes VMs, networks, IPs, and all other infrastructure"
        echo ""
        read -p "Type 'DESTROY' to confirm: " confirm
        if [ "$confirm" != "DESTROY" ]; then
            echo "❌ Destruction cancelled"
            exit 1
        fi
    fi

    init_terraform

    # Try to check state, but continue even if it fails
    check_terraform_state || echo "⚠️  Could not analyze Terraform state"

    # Force destroy if we have state, otherwise just cleanup
    if [ -f "terraform.tfstate" ] || terraform state list >/dev/null 2>&1; then
        force_destroy
    else
        echo "📋 No Terraform state found, proceeding with manual cleanup..."
    fi

    # Clean up any remaining resources
    cleanup_remaining

    # Remove state files for fresh start
    remove_state_files

    # Remove generated tfvars
    remove_tfvars

    echo ""
    echo "🎉 Complete destruction finished!"
    echo "Your environment is now clean and ready for fresh deployment."
}

# Help message
show_help() {
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  --force     Skip confirmation prompts"
    echo "  --help      Show this help message"
    echo ""
    echo "Environment variables:"
    echo "  TERRAFORM_DIR    Terraform directory (default: terraform)"
    echo "  FORCE_DESTROY    Set to 'true' to skip confirmation (default: false)"
    echo ""
    echo "Examples:"
    echo "  $0                           # Interactive destruction"
    echo "  $0 --force                   # Force destruction without confirmation"
    echo "  FORCE_DESTROY=true $0        # Force destruction via environment variable"
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --force)
            export FORCE_DESTROY=true
            shift
            ;;
        --help)
            show_help
            exit 0
            ;;
        *)
            echo "❌ Unknown option: $1"
            show_help
            exit 1
            ;;
    esac
done

# Run main function
main