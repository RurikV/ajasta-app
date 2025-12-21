#!/bin/bash
# Workspace management script for Terraform environments
# Usage: ./manage-workspaces.sh [command] [workspace-name]

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check if terraform is installed
check_terraform() {
    if ! command -v terraform &> /dev/null; then
        print_error "Terraform is not installed or not in PATH"
        exit 1
    fi
}

# Function to check if workspace exists
workspace_exists() {
    local workspace_name=$1
    terraform workspace list | grep -q "^$workspace_name$"
}

# Function to initialize terraform
init_terraform() {
    print_status "Initializing Terraform..."
    terraform init
    print_success "Terraform initialized"
}

# Function to create workspace
create_workspace() {
    local workspace_name=$1
    local var_file="${workspace_name}.tfvars"

    if workspace_exists "$workspace_name"; then
        print_warning "Workspace '$workspace_name' already exists"
        return
    fi

    print_status "Creating workspace: $workspace_name"
    terraform workspace new "$workspace_name"
    terraform workspace select "$workspace_name"

    # Apply workspace-specific variables
    if [[ -f "$var_file" ]]; then
        print_status "Applying variables from $var_file"
        terraform plan -var-file="$var_file" -out="${workspace_name}-plan.tfplan"
        print_status "Plan created. Review the plan and run 'terraform apply' to deploy."
    else
        print_warning "Variables file $var_file not found. You'll need to provide variables manually."
    fi
}

# Function to select workspace
select_workspace() {
    local workspace_name=$1

    if ! workspace_exists "$workspace_name"; then
        print_error "Workspace '$workspace_name' does not exist"
        echo "Available workspaces:"
        terraform workspace list
        exit 1
    fi

    terraform workspace select "$workspace_name"
    print_success "Switched to workspace: $workspace_name"
    print_status "Current workspace: $(terraform workspace show)"
}

# Function to list workspaces
list_workspaces() {
    print_status "Available Terraform workspaces:"
    terraform workspace list
    print_status "Current workspace: $(terraform workspace show)"
}

# Function to delete workspace
delete_workspace() {
    local workspace_name=$1

    if [[ "$workspace_name" == "default" ]]; then
        print_error "Cannot delete 'default' workspace"
        exit 1
    fi

    if ! workspace_exists "$workspace_name"; then
        print_error "Workspace '$workspace_name' does not exist"
        exit 1
    fi

    # Check if we're trying to delete the current workspace
    if [[ "$(terraform workspace show)" == "$workspace_name" ]]; then
        terraform workspace select default
    fi

    print_warning "This will permanently delete the workspace '$workspace_name' and all its state"
    read -p "Are you sure you want to continue? (yes/no): " confirm

    if [[ "$confirm" != "yes" ]]; then
        print_status "Operation cancelled"
        exit 0
    fi

    terraform workspace delete "$workspace_name"
    print_success "Workspace '$workspace_name' deleted"
}

# Function to show workspace status
show_status() {
    local current_workspace=$(terraform workspace show)
    print_status "Current workspace: $current_workspace"

    # Load workspace-specific configuration
    local var_file="${current_workspace}.tfvars"
    if [[ -f "$var_file" ]]; then
        print_status "Variables file: $var_file"

        # Extract some key variables to display
        if grep -q "environment" "$var_file"; then
            print_status "Environment: $(grep environment "$var_file" | cut -d'=' -f2 | tr -d ' "')"
        fi
        if grep -q "worker_count" "$var_file"; then
            print_status "Worker count: $(grep worker_count "$var_file" | cut -d'=' -f2 | tr -d ' "')"
        fi
        if grep -q "preemptible" "$var_file"; then
            print_status "Preemptible: $(grep preemptible "$var_file" | cut -d'=' -f2 | tr -d ' "')"
        fi
    else
        print_warning "No variables file found for current workspace"
    fi

    # Show any existing state
    if terraform state list >/dev/null 2>&1; then
        print_status "Resources in state: $(terraform state list | wc -l)"
    else
        print_status "No state resources found"
    fi
}

# Function to plan with workspace-specific variables
plan_workspace() {
    local workspace_name=$1
    local var_file="${workspace_name}.tfvars"

    select_workspace "$workspace_name"

    if [[ -f "$var_file" ]]; then
        print_status "Planning with variables from $var_file"
        terraform plan -var-file="$var_file" -out="${workspace_name}-plan.tfplan"
        print_success "Plan created: ${workspace_name}-plan.tfplan"
    else
        print_error "Variables file $var_file not found"
        exit 1
    fi
}

# Function to apply with workspace-specific variables
apply_workspace() {
    local workspace_name=$1
    local var_file="${workspace_name}.tfvars"
    local plan_file="${workspace_name}-plan.tfplan"

    select_workspace "$workspace_name"

    if [[ ! -f "$plan_file" ]]; then
        print_status "No plan file found. Creating plan first..."
        plan_workspace "$workspace_name"
    fi

    print_warning "Applying changes to workspace: $workspace_name"
    terraform apply "$plan_file"
    print_success "Applied changes to workspace: $workspace_name"
}

# Main script logic
main() {
    check_terraform

    case "${1:-}" in
        "init")
            init_terraform
            ;;
        "create")
            if [[ -z "${2:-}" ]]; then
                print_error "Usage: $0 create <workspace-name>"
                exit 1
            fi
            init_terraform
            create_workspace "$2"
            ;;
        "select")
            if [[ -z "${2:-}" ]]; then
                print_error "Usage: $0 select <workspace-name>"
                exit 1
            fi
            select_workspace "$2"
            ;;
        "list")
            list_workspaces
            ;;
        "delete")
            if [[ -z "${2:-}" ]]; then
                print_error "Usage: $0 delete <workspace-name>"
                exit 1
            fi
            delete_workspace "$2"
            ;;
        "status")
            show_status
            ;;
        "plan")
            if [[ -z "${2:-}" ]]; then
                print_error "Usage: $0 plan <workspace-name>"
                exit 1
            fi
            init_terraform
            plan_workspace "$2"
            ;;
        "apply")
            if [[ -z "${2:-}" ]]; then
                print_error "Usage: $0 apply <workspace-name>"
                exit 1
            fi
            init_terraform
            apply_workspace "$2"
            ;;
        *)
            echo "Terraform Workspace Management Script"
            echo ""
            echo "Usage: $0 <command> [workspace-name]"
            echo ""
            echo "Commands:"
            echo "  init                       Initialize Terraform"
            echo "  create <workspace>         Create a new workspace"
            echo "  select <workspace>         Select a workspace"
            echo "  list                       List all workspaces"
            echo "  delete <workspace>         Delete a workspace"
            echo "  status                     Show current workspace status"
            echo "  plan <workspace>           Create plan for workspace"
            echo "  apply <workspace>          Apply changes to workspace"
            echo ""
            echo "Examples:"
            echo "  $0 init"
            echo "  $0 create staging"
            echo "  $0 plan staging"
            echo "  $0 apply staging"
            echo "  $0 select production"
            echo "  $0 status"
            exit 1
            ;;
    esac
}

# Run main function with all arguments
main "$@"