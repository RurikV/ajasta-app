# Production Environment Variables
# This file contains configuration specific to production environment

# Environment identification
environment = "production"
prefix      = "ajasta-prod"

# Yandex Cloud zone
yc_zone = "ru-central1-b"

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
ssh_username = "ajasta"
ssh_public_key_file = "../ssh-key-id_rsa_k8s.pub"  # Path to SSH public key file (relative to terraform/ directory)

# Production labels and tags
labels = {
  environment         = "production"
  project             = "ajasta-app"
  managed_by          = "terraform"
  cost_center         = "business"
  data_classification = "confidential"
}
