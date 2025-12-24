# Production Environment Variables
# This file contains configuration specific to production environment

# Environment identification
environment = "production"
prefix      = "ajasta-prod"

# Production VM Configuration
master_core_fraction = 50
worker_core_fraction = 50
master_vm_memory     = 8
master_vm_cores      = 4
worker_vm_cores      = 4
worker_count         = 3 # Full cluster for production

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
