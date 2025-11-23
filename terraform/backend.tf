# Terraform Backend Configuration
# GitLab CI/CD will use HTTP backend
# For local development, use setup-local-terraform.sh script

terraform {
  # Try GitLab's built-in backend first
  backend "gitlab" {
    # These will be configured automatically by GitLab CI/CD
    # If this doesn't work, we'll fall back to manual HTTP configuration
  }
}