# Terraform Workspace Configuration
# This file configures different environments (staging, production)
# Note: With GitLab CI/CD HTTP backend, we use state files instead of workspaces
# This configuration provides fallback for local development

locals {
  # Environment-specific configuration
  environments = {
    staging = {
      # Resource naming
      prefix = "ajasta-staging"

      # VM Configuration for staging
      master_core_fraction = 20
      worker_core_fraction = 20
      master_memory        = 4
      worker_memory        = 4
      master_cores         = 2
      worker_cores         = 2

      # Network configuration
      external_network_cidr = "172.16.18.0/28"
      internal_network_cidr = "10.10.1.0/24"

      # Cost optimization for staging
      preemptible    = true
      boot_disk_size = 20

      # Tags
      tags = {
        environment = "staging"
        cost_center = "development"
      }
    }

    production = {
      # Resource naming
      prefix = "ajasta-prod"

      # VM Configuration for production
      master_core_fraction = 50
      worker_core_fraction = 50
      master_memory        = 8
      worker_memory        = 8
      master_cores         = 4
      worker_cores         = 4

      # Network configuration
      external_network_cidr = "172.16.19.0/28"
      internal_network_cidr = "10.10.2.0/24"

      # Production settings
      preemptible    = false
      boot_disk_size = 50

      # Tags
      tags = {
        environment = "production"
        cost_center = "business"
      }
    }
  }

  # Current environment configuration
  # Use lookup() to handle "default" workspace (fallback to staging)
  current_env = lookup(local.environments, terraform.workspace, local.environments["staging"])

  # Common tags for all resources
  common_tags = merge(
    {
      project    = "ajasta-app"
      managed_by = "terraform"
      workspace  = terraform.workspace
    },
    local.current_env.tags
  )
}

# Output current workspace info
output "current_workspace" {
  value = terraform.workspace
}

output "environment_config" {
  value = local.current_env
}