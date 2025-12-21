# Terraform Backend Configuration
# GitLab CI/CD will use HTTP backend with workspace-specific state files
# For local development, use setup-local-terraform.sh script

terraform {
  backend "http" {
    # These will be configured by GitLab CI/CD environment variables
    # Each workspace will have its own state file
    # address        = "${TF_HTTP_ADDRESS}"  # Will be constructed as: ${CI_API_V4_URL}/projects/${CI_PROJECT_ID}/terraform/state/${TF_STATE_NAME}
    # lock_address   = "${TF_HTTP_LOCK_ADDRESS}"  # Will be constructed as: ${CI_API_V4_URL}/projects/${CI_PROJECT_ID}/terraform/state/${TF_STATE_NAME}/lock
    # unlock_address = "${TF_HTTP_UNLOCK_ADDRESS}"  # Will be constructed as: ${CI_API_V4_URL}/projects/${CI_PROJECT_ID}/terraform/state/${TF_STATE_NAME}/lock
    # username       = "${TF_HTTP_USERNAME}"  # gitlab-ci-token
    # password       = "${TF_HTTP_PASSWORD}"  # ${CI_JOB_TOKEN}
    # lock_method    = "POST"
    # unlock_method  = "POST"
    # retry_wait_min = 5
  }

  required_version = ">= 1.5.0"
}