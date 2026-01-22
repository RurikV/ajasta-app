# Terraform configuration for Monitoring Stack Infrastructure
# Manages Longhorn storage and resources for Prometheus and Grafana

terraform {
  required_version = ">= 1.0"

  required_providers {
    yandex = {
      source  = "yandex-cloud/yandex"
      version = ">= 0.115"
    }
  }

  backend "http" {
    # Configuration will be provided via GitLab CI/CD
  }
}

provider "yandex" {
  token     = var.yc_token
  cloud_id  = var.yc_cloud_id
  folder_id = var.yc_folder_id
}

# Variables
variable "yc_cloud_id" {
  type        = string
  description = "Yandex Cloud ID"
}

variable "yc_folder_id" {
  type        = string
  description = "Yandex Cloud Folder ID"
}

variable "yc_token" {
  type        = string
  description = "Yandex Cloud OAuth token"
  default     = ""
}

variable "cluster_name" {
  type        = string
  description = "Cluster name for resource tagging"
  default     = "ajasta-monitoring"
}

variable "prometheus_storage_size" {
  type        = number
  description = "Prometheus storage size in GB"
  default     = 50
}

variable "grafana_storage_size" {
  type        = number
  description = "Grafana storage size in GB"
  default     = 10
}

variable "alertmanager_storage_size" {
  type        = number
  description = "Alertmanager storage size in GB"
  default     = 2
}

variable "enable_monitoring_backup" {
  type        = bool
  description = "Enable backup for monitoring data"
  default     = false
}

# Locals
locals {
  tags = {
    Project     = "ajasta"
    Component   = "monitoring"
    ManagedBy   = "terraform"
    Environment = "production"
  }
}
