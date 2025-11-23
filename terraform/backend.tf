# Terraform Backend Configuration
# GitLab CI/CD will use HTTP backend
# For local development, use setup-local-terraform.sh script

terraform {
  backend "http" {}
}