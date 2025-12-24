# Staging Environment Variables
# This file contains configuration specific to staging environment

# Environment identification
environment = "staging"
prefix      = "ajasta-staging"

# Yandex Cloud configuration
yc_cloud_id  = "your-cloud-id-here"
yc_folder_id = "your-staging-folder-id-here"
yc_zone      = "ru-central1-b"

# Staging VM Configuration
master_core_fraction = 20
worker_core_fraction = 20
master_vm_memory     = 6
master_vm_cores      = 2
worker_vm_cores      = 2
worker_count         = 1 # Reduced for staging

# Network configuration for staging
external_network_cidr = "172.16.18.0/28"
internal_network_cidr = "10.10.1.0/24"

# Staging resource settings
preemptible    = true
boot_disk_size = 20
boot_disk_type = "network-hdd"

# SSH access
ssh_username   = "ajasta"
ssh_public_key = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABgQC... your-staging-public-key"

# Staging labels and tags
labels = {
  environment = "staging"
  project     = "ajasta-app"
  managed_by  = "terraform"
  cost_center = "development"
}
