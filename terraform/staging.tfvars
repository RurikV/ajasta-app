# Staging Environment Variables
# This file contains configuration specific to staging environment

# Environment identification
environment = "staging"
prefix      = "ajasta-staging"

# Yandex Cloud zone
yc_zone = "ru-central1-b"

# Staging VM Configuration
master_core_fraction = 20
worker_core_fraction = 20
master_vm_memory     = 6
master_vm_cores      = 2
worker_vm_cores      = 2
worker_count         = 1 # Reduced for staging

# Staging resource settings
preemptible    = true
boot_disk_size = 20
boot_disk_type = "network-hdd"

# SSH access
ssh_username        = "ajasta"
ssh_public_key_file = "../ssh-key-id_rsa_k8s.pub" # Path to SSH public key file (relative to terraform/ directory)

# Staging labels and tags
labels = {
  environment = "staging"
  project     = "ajasta-app"
  managed_by  = "terraform"
  cost_center = "development"
}
