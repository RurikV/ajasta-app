# Terraform Backend Configuration
# GitLab CI/CD will use HTTP backend
# For local development, use setup-local-terraform.sh script

terraform {
  backend "http" {
    # These will be configured by GitLab CI/CD environment variables
    # address        = "${TF_HTTP_ADDRESS}"
    # lock_address   = "${TF_HTTP_LOCK_ADDRESS}"
    # unlock_address = "${TF_HTTP_UNLOCK_ADDRESS}"
    # username       = "${TF_HTTP_USERNAME}"
    # password       = "${TF_HTTP_PASSWORD}"
    # lock_method    = "POST"
    # unlock_method  = "POST"
    # retry_wait_min = 5
  }
}