# Bind Permanent IP to Kubernetes Master
# This configuration binds the permanent IP 178.154.197.121 to the k8s-master node
# instead of a worker node, making it the primary access point for the cluster

# Update the master instance to use the permanent DNS IP
resource "yandex_compute_instance" "master" {
  name        = var.master_vm_name
  hostname    = var.master_vm_name
  zone        = var.yc_zone
  platform_id = "standard-v3"

  resources {
    cores         = var.master_vm_cores
    memory        = var.master_vm_memory
    core_fraction = var.core_fraction
  }

  scheduling_policy {
    preemptible = var.preemptible
  }

  boot_disk {
    initialize_params {
      image_id = data.yandex_compute_image.centos_stream9_oslogin.id
      size     = var.master_vm_disk_size
      type     = "network-hdd"
    }
  }

  # Use the permanent DNS IP instead of the regular master IP
  network_interface {
    subnet_id      = yandex_vpc_subnet.internal.id
    nat            = true
    nat_ip_address = yandex_vpc_address.production_dns.external_ipv4_address[0].address
  }

  metadata = merge(local.base_metadata, local.ssh_metadata)

  # Ensure the master with permanent IP is protected
  lifecycle {
    prevent_destroy = true
  }
}

# Output the master's permanent IP
output "master_permanent_ip" {
  description = "Master node permanent IP address (178.154.197.121)"
  value       = yandex_vpc_address.production_dns.external_ipv4_address[0].address
}

# Output the master's instance ID
output "master_instance_id" {
  description = "Master node instance ID"
  value       = yandex_compute_instance.master.id
}
