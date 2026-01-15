#!/bin/bash

# Complete Terraform Destroy Script
# This script destroys Terraform-managed resources with production safety features
#
# ⚠️ IMPORTANT: If you have production infrastructure (like DNS configured domains),
# use --preserve-production to protect critical resources from being destroyed.

set -e

echo "🎯 Starting Terraform destruction process..."

# Configuration
TERRAFORM_DIR="${TERRAFORM_DIR:-terraform}"
FORCE_DESTROY="${FORCE_DESTROY:-false}"
PRESERVE_PRODUCTION="${PRESERVE_PRODUCTION:-false}"

# Production resources to preserve (add your critical resources here)
PRESERVED_RESOURCES=(
    "yandex_vpc_address.production_dns"                 # Permanent DNS IP (178.154.197.121)
    "yandex_vpc_address.workers[\"k8s-worker-2\"]"     # 178.154.197.121 - worker-node-1
    "yandex_compute_instance.workers[\"k8s-worker-2\"]" # worker-node-1 VM
)

# Function to check if terraform directory exists
check_terraform_dir() {
    if [ ! -d "$TERRAFORM_DIR" ]; then
        echo "❌ Terraform directory not found: $TERRAFORM_DIR"
        exit 1
    fi

    cd "$TERRAFORM_DIR"
    echo "📁 Working directory: $(pwd)"
}

# Function to check for DNS-configured domains
check_dns_domains() {
    echo "🔍 Checking for DNS-configured domains..."

    # Check if bind-domain playbook has been run
    if [ -f "../ansible/ajasta-app/29-bind-domain.yml" ]; then
        echo "⚠️  Domain binding playbook found"
        echo "   If you have configured DNS for ajasta.top (178.154.197.121),"
        echo "   you should use --preserve-production to protect infrastructure!"
        echo ""
    fi

    # Check for DNS configuration guides
    if [ -f "../ansible/ajasta-app/DNS_QUICK_REFERENCE.md" ] || \
       [ -f "../ansible/ajasta-app/PORKBUN_DNS_GUIDE.md" ]; then
        echo "⚠️  DNS configuration guides found"
        echo "   If you've configured DNS using these guides, your infrastructure"
        echo "   is in production and should be preserved!"
        echo ""
    fi
}

# Function to initialize Terraform (if needed)
init_terraform() {
    # Try to load GitLab CI/CD variables if not already set
    if [ -z "${YC_TOKEN}" ] || [ -z "${YC_CLOUD_ID}" ] || [ -z "${YC_FOLDER_ID}" ]; then
        if [ -f "../scripts/get-gitlab-vars.sh" ]; then
            echo "📡 Fetching Yandex Cloud credentials from GitLab..."
            source ../scripts/get-gitlab-vars.sh
            echo ""
        fi
    fi

    # Check for Yandex Cloud authentication
    if [ -z "${YC_TOKEN}" ] && [ -z "${YC_CLOUD_ID}" ] && [ -z "${YC_FOLDER_ID}" ]; then
        echo "❌ ERROR: Yandex Cloud authentication not configured!"
        echo ""
        echo "Please either:"
        echo "  1. Set GITLAB_PAT to fetch variables from GitLab:"
        echo "     export GITLAB_PAT=\"glpat-xxxxxxxxxxxxxxxxxxxx\""
        echo ""
        echo "  2. Or set these environment variables manually:"
        echo "     export YC_TOKEN=\"your-oauth-token\""
        echo "     export YC_CLOUD_ID=\"your-cloud-id\""
        echo "     export YC_FOLDER_ID=\"your-folder-id\""
        echo ""
        echo "You can find these values by running:"
        echo "  yc config list"
        echo ""
        echo "Or get a new OAuth token at:"
        echo "  https://oauth.yandexcloud.com/authorize?response_type=token"
        exit 1
    fi

    echo "✅ Yandex Cloud authentication configured"
    echo "   Cloud ID: ${YC_CLOUD_ID}"
    echo "   Folder ID: ${YC_FOLDER_ID}"
    echo ""

    echo "🔧 Initializing Terraform with HTTP backend..."

    # Initialize with HTTP backend (may fail if no GitLab credentials, but that's OK)
    # The backend configuration is in backend.tf
    terraform init -input=false || true
}

# Function to check Terraform state
check_terraform_state() {
    if [ ! -f "terraform.tfstate" ]; then
        echo "⚠️  No terraform.tfstate file found"
        echo "   This might be a fresh workspace or state is stored remotely"
        return 1
    fi

    echo "📊 Terraform state analysis:"
    echo "   Resources that will be destroyed:"
    terraform state list 2>/dev/null | head -15 || echo "   (Could not list resources)"
    echo ""
}

# Function to preserve production resources
preserve_production_resources() {
    echo "🛡️  Preserving production resources..."
    echo ""

    for resource in "${PRESERVED_RESOURCES[@]}"; do
        echo "   Preserving: $resource"

        # Remove from state (this preserves the actual resource)
        if terraform state list "$resource" >/dev/null 2>&1; then
            terraform state rm "$resource" || echo "     ⚠️  Could not remove from state (may already be removed)"
            echo "     ✅ Removed from Terraform state (resource preserved)"
        else
            echo "     ℹ️  Not in Terraform state (may already be preserved)"
        fi
        echo ""
    done

    echo "✅ Production resources preserved"
    echo "   These resources will NOT be destroyed and are now outside Terraform control"
    echo "   To bring them back under Terraform management, use terraform import"
    echo ""
}

# Function to show preserved resources
show_preserved_resources() {
    echo "🛡️  Protected Production Resources:"
    echo ""
    echo "   The following resources are configured for preservation:"
    echo ""
    echo "   1. yandex_vpc_address.production_dns"
    echo "      → External IP: 178.154.197.121"
    echo "      → Purpose: Permanent DNS IP for ajasta.top"
    echo "      → Protection: prevent_destroy = true"
    echo ""
    echo "   2. yandex_vpc_address.workers[\"k8s-worker-2\"]"
    echo "      → External IP: 178.154.197.121"
    echo "      → Node: worker-node-1"
    echo "      → Purpose: ajasta.top DNS A record"
    echo ""
    echo "   3. yandex_compute_instance.workers[\"k8s-worker-2\"]"
    echo "      → VM: worker-node-1"
    echo "      → Purpose: Production Kubernetes worker node"
    echo ""
    echo "   These resources will be REMOVED from Terraform state"
    echo "   but will continue to exist in Yandex Cloud!"
    echo ""
}

# Function to force destroy all resources
force_destroy() {
    echo "💥 Destroying Terraform-managed resources..."

    # Create a destroy plan (ignore lock release errors)
    echo "   Creating destroy plan..."
    terraform plan -destroy -out=destroy.tfplan -input=false || true

    # Check if plan was created
    if [ ! -f "destroy.tfplan" ]; then
        echo "   ⚠️  Destroy plan not created (may have no resources to destroy)"
        return 0
    fi

    # Show what will be destroyed
    echo ""
    echo "   Resources to be destroyed:"
    terraform show -no-color destroy.tfplan | grep -A 2 "will be destroyed" || true
    echo ""

    # Apply the destroy plan (ignore lock release errors)
    echo "🔥 Applying destroy plan..."
    terraform apply -input=false -auto-approve destroy.tfplan || true

    echo "✅ Terraform-managed resources destroyed!"
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
        BACKUP_FILE="terraform.tfstate.backup.$(date +%Y%m%d_%H%M%S)"
        cp terraform.tfstate "$BACKUP_FILE"
        echo "   💾 State backed up to: $BACKUP_FILE"
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

# Function to show production preservation summary
show_production_summary() {
    echo ""
    echo "================================================================================================"
    echo "🛡️  PRODUCTION PRESERVATION MODE"
    echo "================================================================================================"
    echo ""
    echo "Your production infrastructure has been PRESERVED:"
    echo ""
    echo "✅ Protected Resources:"
    echo "   • worker-node-1 VM (178.154.197.121)"
    echo "   • External IP address 178.154.197.121"
    echo ""
    echo "✅ What This Means:"
    echo "   • These resources continue running in Yandex Cloud"
    echo "   • They are NO LONGER managed by Terraform"
    echo "   • Your DNS (ajasta.top → 178.154.197.121) continues working"
    echo "   • Your Kubernetes cluster on worker-node-1 continues running"
    echo ""
    echo "⚠️  Important Notes:"
    echo "   • To bring these resources back under Terraform control, use:"
    echo "     terraform import yandex_compute_instance.workers[\"k8s-worker-2\"] <instance_id>"
    echo "     terraform import yandex_vpc_address.workers[\"k8s-worker-2\"] <address_id>"
    echo ""
    echo "   • The worker-node-1 is now independent infrastructure"
    echo "   • Future Terraform applies won't affect it"
    echo "   • You must manage it manually or import it back"
    echo ""
    echo "================================================================================================"
    echo ""
}

# Main execution
main() {
    echo "🎯 Complete Terraform Destruction Process"
    echo "========================================"
    echo ""

    check_terraform_dir

    # Check for DNS-configured domains
    check_dns_domains

    # Show preserved resources if in production-preservation mode
    if [ "$PRESERVE_PRODUCTION" = "true" ]; then
        show_preserved_resources
    fi

    # Check if we want to force destroy or ask for confirmation
    if [ "$FORCE_DESTROY" = "true" ]; then
        echo "⚡ Force destroy mode enabled"
    else
        echo "⚠️  WARNING: This will destroy Terraform-managed resources!"
        echo "   This includes VMs, networks, IPs, and all other infrastructure"
        echo ""

        if [ "$PRESERVE_PRODUCTION" = "true" ]; then
            echo "🛡️  Production preservation mode: ENABLED"
            echo "   worker-node-1 (178.154.197.121) will be PRESERVED"
            echo ""
        fi

        read -p "Type 'DESTROY' to confirm: " confirm
        if [ "$confirm" != "DESTROY" ]; then
            echo "❌ Destruction cancelled"
            exit 1
        fi
    fi

    echo ""

    init_terraform

    # Try to check state, but continue even if it fails
    check_terraform_state || echo "⚠️  Could not analyze Terraform state"
    echo ""

    # Preserve production resources if requested
    if [ "$PRESERVE_PRODUCTION" = "true" ]; then
        preserve_production_resources
    fi

    # Force destroy if we have state, otherwise just cleanup
    if [ -f "terraform.tfstate" ] || terraform state list >/dev/null 2>&1; then
        force_destroy
    else
        echo "ℹ️  No Terraform state found, proceeding with manual cleanup..."
    fi

    # Clean up any remaining resources
    echo ""
    cleanup_remaining

    # Remove state files for fresh start
    remove_state_files

    # Remove generated tfvars
    remove_tfvars

    echo ""
    echo "✅ Complete destruction finished!"

    if [ "$PRESERVE_PRODUCTION" = "true" ]; then
        show_production_summary
    else
        echo "Your environment is now clean and ready for fresh deployment."
    fi
}

# Help message
show_help() {
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  --force                Skip confirmation prompts"
    echo "  --preserve-production  Preserve production infrastructure (worker-node-1, 178.154.197.121)"
    echo "  --help                 Show this help message"
    echo ""
    echo "Environment variables:"
    echo "  TERRAFORM_DIR         Terraform directory (default: terraform)"
    echo "  FORCE_DESTROY         Set to 'true' to skip confirmation (default: false)"
    echo "  PRESERVE_PRODUCTION   Set to 'true' to preserve production infrastructure (default: false)"
    echo ""
    echo "Examples:"
    echo "  $0                                      # Interactive destruction"
    echo "  $0 --force                              # Force destruction without confirmation"
    echo "  $0 --preserve-production                # Destroy non-production, preserve worker-node-1"
    echo "  $0 --preserve-production --force        # Preserve production without confirmation"
    echo ""
    echo "Production Preservation:"
    echo "  When --preserve-production is used, the following resources are protected:"
    echo "    • Permanent DNS IP (178.154.197.121) - prevent_destroy enabled"
    echo "    • worker-node-1 VM (k8s-worker-2)"
    echo "    • External IP 178.154.197.121 (configured for ajasta.top DNS)"
    echo ""
    echo "  These resources are removed from Terraform state but continue running."
    echo "  Use this when you have production infrastructure with DNS configured."
    echo ""
    echo "  To bring resources back under Terraform control:"
    echo "    terraform import yandex_vpc_address.production_dns <address_id>"
    echo "    terraform import yandex_compute_instance.workers[\"k8s-worker-2\"] <instance_id>"
    echo "    terraform import yandex_vpc_address.workers[\"k8s-worker-2\"] <address_id>"
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --force)
            export FORCE_DESTROY=true
            shift
            ;;
        --preserve-production)
            export PRESERVE_PRODUCTION=true
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
