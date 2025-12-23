# Production Environment Variables
# This file contains configuration specific to production environment

# Environment identification
environment = "production"
prefix      = "ajasta-prod"

# Yandex Cloud configuration
yc_cloud_id  = "your-cloud-id-here"
yc_folder_id = "your-production-folder-id-here"
yc_zone      = "ru-central1-b"

# Production VM Configuration
master_core_fraction = 50
worker_core_fraction = 50
master_memory        = 8
worker_memory        = 8
master_cores         = 4
worker_cores         = 4
worker_count         = 3 # Full cluster for production

# Network configuration for production
external_network_cidr = "172.16.19.0/28"
internal_network_cidr = "10.10.2.0/24"

# Production resource settings
preemptible    = false
boot_disk_size = 50
boot_disk_type = "network-ssd"

# SSH access
ssh_username   = "ajasta"
ssh_public_key = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABgQC... your-production-public-key"

# Production labels and tags
labels = {
  environment         = "production"
  project             = "ajasta-app"
  managed_by          = "terraform"
  cost_center         = "business"
  data_classification = "confidential"
}

# Application settings for production
app_environment = "production"
app_replicas    = 3
app_resources = {
  cpu_limit      = "2000m"
  memory_limit   = "2Gi"
  cpu_request    = "1000m"
  memory_request = "1Gi"
}

# Database settings for production
db_instance_class       = "db.t3.medium"
db_storage_size         = 100
db_multi_az             = true
backup_retention_period = 30