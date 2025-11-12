variable "yc_cloud_id" {
  description = "Yandex Cloud cloud ID"
  type        = string
}

variable "yc_folder_id" {
  description = "Yandex Cloud folder ID"
  type        = string
}

variable "yc_zone" {
  description = "Default availability zone"
  type        = string
  default     = "ru-central1-b"
}

variable "yc_service_account_key_file" {
  description = "Path to Yandex Cloud service account key JSON (optional if using YC_TOKEN or metadata)"
  type        = string
  default     = ""
}

provider "yandex" {
  cloud_id  = var.yc_cloud_id
  folder_id = var.yc_folder_id
  zone      = var.yc_zone
  # If empty, provider will rely on YC_TOKEN/IMDS
  service_account_key_file = var.yc_service_account_key_file != "" ? var.yc_service_account_key_file : null
}
