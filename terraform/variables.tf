# Environment-specific variables
variable "environment" {
  description = "Environment name (staging, production)"
  type        = string
  default     = "development"
}

variable "prefix" {
  description = "Prefix for resource names"
  type        = string
  default     = "ajasta"
}

variable "yc_network_name" {
  description = "External VPC network name"
  type        = string
  default     = "external-ajasta-network"
}

variable "yc_subnet_name" {
  description = "External subnet name"
  type        = string
  default     = "ajasta-external-segment"
}

variable "yc_subnet_cidr" {
  description = "External subnet CIDR"
  type        = string
  default     = "172.16.17.0/28"
}

variable "yc_internal_network_name" {
  description = "Internal VPC network name"
  type        = string
  default     = "internal-ajasta-network"
}

variable "yc_internal_subnet_name" {
  description = "Internal subnet name"
  type        = string
  default     = "ajasta-internal-segment"
}

variable "yc_internal_subnet_cidr" {
  description = "Internal subnet CIDR"
  type        = string
  default     = "10.10.0.0/24"
}

variable "yc_sa_name" {
  description = "Service account name (optional to create)"
  type        = string
  default     = "otus"
}

variable "create_service_account" {
  description = "Whether to create service account and static key"
  type        = bool
  default     = false
}

variable "master_address_name" {
  description = "Static public IP resource name for master"
  type        = string
  default     = "ajasta-k8s-master-ip"
}

variable "workers" {
  description = "Workers definition: list of objects with vm_name and address_name"
  type = list(object({
    vm_name      = string
    address_name = string
  }))
  default = [
    { vm_name = "k8s-worker-1", address_name = "ajasta-k8s-worker1-ip" },
    { vm_name = "k8s-worker-2", address_name = "ajasta-k8s-worker2-ip" },
    { vm_name = "k8s-worker-3", address_name = "ajasta-k8s-worker3-ip" },
  ]
}

variable "master_vm_name" {
  description = "Master VM name"
  type        = string
  default     = "k8s-master"
}

variable "ssh_username" {
  description = "SSH username to authorize on instances"
  type        = string
  default     = "ajasta"
}

variable "ssh_public_key" {
  description = "SSH public key content to inject (e.g. file(\"~/.ssh/id_rsa.pub\"))"
  type        = string
  default     = ""
}

variable "ssh_public_key_file" {
  description = "Path to SSH public key file (use this instead of calling file() in tfvars). Supports ~ via pathexpand()."
  type        = string
  default     = ""
}

variable "metadata_yaml" {
  description = "Path to cloud-init user-data YAML"
  type        = string
  default     = "../scripts/metadata.yaml"
}

variable "master_vm_memory" {
  description = "Master RAM in GB"
  type        = number
  default     = 6
}

variable "master_vm_cores" {
  description = "Master CPU cores"
  type        = number
  default     = 2
}

variable "master_vm_disk_size" {
  description = "Master boot disk size in GB"
  type        = number
  default     = 30
}

variable "worker_vm_memory" {
  description = "Worker RAM in GB"
  type        = number
  default     = 6
}

variable "worker_vm_cores" {
  description = "Worker CPU cores"
  type        = number
  default     = 2
}

variable "worker_vm_disk_size" {
  description = "Worker boot disk size in GB"
  type        = number
  default     = 30
}

# New environment management variables
variable "worker_count" {
  description = "Number of worker nodes"
  type        = number
  default     = 3
}

variable "preemptible" {
  description = "Whether VMs should be preemptible"
  type        = bool
  default     = true
}

variable "boot_disk_type" {
  description = "Boot disk type (network-hdd or network-ssd)"
  type        = string
  default     = "network-hdd"
}

variable "boot_disk_size" {
  description = "Boot disk size in GB"
  type        = number
  default     = 30
}

variable "master_core_fraction" {
  description = "Master CPU core fraction (5, 20, 50, 100)"
  type        = number
  default     = 20
}

variable "worker_core_fraction" {
  description = "Worker CPU core fraction (5, 20, 50, 100)"
  type        = number
  default     = 20
}

variable "labels" {
  description = "Labels to apply to all resources"
  type        = map(string)
  default     = {}
}

# Application-specific variables
variable "app_replicas" {
  description = "Number of application replicas to deploy"
  type        = number
  default     = 1
}

variable "app_resources" {
  description = "Application resource constraints"
  type = object({
    cpu_limit      = string
    memory_limit   = string
    cpu_request    = string
    memory_request = string
  })
  default = {
    cpu_limit      = "1000m"
    memory_limit   = "1Gi"
    cpu_request    = "500m"
    memory_request = "512Mi"
  }
}
