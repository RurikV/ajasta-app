# Terraform Backend Configuration
# GitLab CI/CD will use HTTP backend with environment-specific state files
# For local development, comment out this backend block or use local backend

terraform {
  required_version = ">= 1.5.0"

  backend "http" {
    # GitLab CI/CD HTTP Backend Configuration
    # These values are automatically provided by GitLab CI/CD pipeline
    #
    # State files per environment:
    # - Staging:  ${CI_API_V4_URL}/projects/${CI_PROJECT_ID}/terraform/state/staging
    # - Production: ${CI_API_V4_URL}/projects/${CI_PROJECT_ID}/terraform/state/production
    #
    # Backend configuration is done via -backend-config flags in CI/CD:
    # address        = "${TF_HTTP_ADDRESS}"
    # lock_address   = "${TF_HTTP_LOCK_ADDRESS}"
    # unlock_address = "${TF_HTTP_UNLOCK_ADDRESS}"
    # username       = "${TF_HTTP_USERNAME}"  # gitlab-ci-token
    # password       = "${TF_HTTP_PASSWORD}"  # ${CI_JOB_TOKEN}
    # lock_method    = "POST"
    # unlock_method  = "POST"
    # retry_wait_min = 5

    # Empty placeholder - actual configuration in CI/CD pipeline
  }
}