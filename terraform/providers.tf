variable "yc_cloud_id" {
  description = "Yandex Cloud cloud ID (defaults to YC_CLOUD_ID environment variable)"
  type        = string
  default     = null
}

variable "yc_folder_id" {
  description = "Yandex Cloud folder ID (defaults to YC_FOLDER_ID environment variable)"
  type        = string
  default     = null
}

variable "yc_zone" {
  description = "Default availability zone (defaults to YC_ZONE environment variable or ru-central1-b)"
  type        = string
  default     = null
}

variable "yc_service_account_key_file" {
  description = "Path to Yandex Cloud service account key JSON (optional if using YC_TOKEN or metadata)"
  type        = string
  default     = ""
}

variable "yc_token" {
  description = "Yandex Cloud IAM/OAuth token (optional, defaults to YC_TOKEN environment variable)"
  type        = string
  default     = null
  sensitive   = true
}

provider "yandex" {
  # Use variables if provided, otherwise fall back to environment variables
  cloud_id  = var.yc_cloud_id != null ? var.yc_cloud_id : null
  folder_id = var.yc_folder_id != null ? var.yc_folder_id : null
  zone      = var.yc_zone != null ? var.yc_zone : "ru-central1-b"
  # Prefer token if provided, otherwise use service account key file or YC_TOKEN env var
  token                    = var.yc_token != null ? var.yc_token : null
  service_account_key_file = var.yc_service_account_key_file != "" ? var.yc_service_account_key_file : null
}
