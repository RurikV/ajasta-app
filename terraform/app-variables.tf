# Variables for Ajasta Application Deployment

variable "deploy_app" {
  description = "Whether to deploy the Ajasta application on the created VMs"
  type        = bool
  default     = true
}

# Docker Registry Configuration
variable "docker_registry" {
  description = "Docker registry URL (e.g., registry.gitlab.com, docker.io)"
  type        = string
  default     = "docker.io"
}

variable "docker_username" {
  description = "Docker registry username"
  type        = string
  default     = ""
}

variable "docker_password" {
  description = "Docker registry password"
  type        = string
  default     = ""
  sensitive   = true
}

# Application Images
variable "backend_image" {
  description = "Docker image for the backend application"
  type        = string
  default     = "vladimirryrik/ajasta-backend:alpine"
}

variable "frontend_image" {
  description = "Docker image for the frontend application"
  type        = string
  default     = "vladimirryrik/ajasta-frontend:alpine"
}

# Application Ports
variable "frontend_port" {
  description = "Port for the frontend application"
  type        = number
  default     = 3000
}

variable "backend_port" {
  description = "Port for the backend API"
  type        = number
  default     = 8090
}

variable "postgres_port" {
  description = "Port for PostgreSQL database"
  type        = number
  default     = 5432
}

# Database Configuration
variable "postgres_db" {
  description = "PostgreSQL database name"
  type        = string
  default     = "ajastadb"
}

variable "postgres_user" {
  description = "PostgreSQL username"
  type        = string
  default     = "admin"
}

variable "postgres_password" {
  description = "PostgreSQL password"
  type        = string
  default     = "adminpw"
  sensitive   = true
}

# Application Secrets
variable "jwt_secret" {
  description = "JWT secret key for authentication"
  type        = string
  default     = "change-me-production-secret-key"
  sensitive   = true
}

# Email Configuration (Optional)
variable "mail_username" {
  description = "Email service username"
  type        = string
  default     = ""
}

variable "mail_password" {
  description = "Email service password"
  type        = string
  default     = ""
  sensitive   = true
}

# AWS Configuration (Optional)
variable "aws_access_key_id" {
  description = "AWS access key ID for S3 integration"
  type        = string
  default     = ""
  sensitive   = true
}

variable "aws_secret_access_key" {
  description = "AWS secret access key for S3 integration"
  type        = string
  default     = ""
  sensitive   = true
}

variable "aws_region" {
  description = "AWS region for S3 integration"
  type        = string
  default     = "us-east-1"
}

variable "aws_s3_bucket" {
  description = "AWS S3 bucket name for file storage"
  type        = string
  default     = ""
}

# Stripe Configuration (Optional)
variable "stripe_public_key" {
  description = "Stripe public key for payment processing"
  type        = string
  default     = ""
}

variable "stripe_secret_key" {
  description = "Stripe secret key for payment processing"
  type        = string
  default     = ""
  sensitive   = true
}

# JVM Configuration
variable "java_opts" {
  description = "JVM options for the Spring Boot backend"
  type        = string
  default     = "-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
}

# SSH Configuration for application deployment
variable "ssh_private_key_file" {
  description = "Path to SSH private key file for accessing VMs"
  type        = string
  default     = "~/.ssh/id_rsa"
}

# Application Configuration
variable "app_domain" {
  description = "Domain name for the application (optional)"
  type        = string
  default     = ""
}

variable "app_environment" {
  description = "Application environment (development, staging, production)"
  type        = string
  default     = "production"

  validation {
    condition     = contains(["development", "staging", "production"], var.app_environment)
    error_message = "Environment must be one of: development, staging, production."
  }
}

variable "enable_ssl" {
  description = "Enable SSL/TLS for the application"
  type        = bool
  default     = false
}

variable "ssl_cert_path" {
  description = "Path to SSL certificate file"
  type        = string
  default     = ""
}

variable "ssl_key_path" {
  description = "Path to SSL private key file"
  type        = string
  default     = ""
}

# Backup Configuration
variable "enable_backups" {
  description = "Enable automatic backups"
  type        = bool
  default     = true
}

variable "backup_retention_days" {
  description = "Number of days to retain backups"
  type        = number
  default     = 7

  validation {
    condition     = var.backup_retention_days >= 1 && var.backup_retention_days <= 30
    error_message = "Backup retention must be between 1 and 30 days."
  }
}

# Monitoring Configuration
variable "enable_monitoring" {
  description = "Enable application monitoring"
  type        = bool
  default     = false
}

variable "monitoring_port" {
  description = "Port for application monitoring endpoints"
  type        = number
  default     = 9090
}

# Scaling Configuration
variable "enable_autoscaling" {
  description = "Enable automatic scaling"
  type        = bool
  default     = false
}

variable "min_replicas" {
  description = "Minimum number of application replicas"
  type        = number
  default     = 1

  validation {
    condition     = var.min_replicas >= 1 && var.min_replicas <= 10
    error_message = "Minimum replicas must be between 1 and 10."
  }
}

variable "max_replicas" {
  description = "Maximum number of application replicas"
  type        = number
  default     = 3

  validation {
    condition     = var.max_replicas >= var.min_replicas && var.max_replicas <= 50
    error_message = "Maximum replicas must be greater than or equal to minimum replicas and less than or equal to 50."
  }
}