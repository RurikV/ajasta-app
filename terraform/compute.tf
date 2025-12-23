data "yandex_compute_image" "centos_stream9_oslogin" {
  family    = "centos-stream-9-oslogin"
  folder_id = "standard-images"
}

locals {
  base_metadata = {
    "serial-port-enable" = 1
    "user-data"          = file(var.metadata_yaml)
  }

  # Resolve SSH public key content:
  # 1) If var.ssh_public_key is provided, use it (trimmed)
  # 2) Else, if var.ssh_public_key_file is provided, read and trim it (supports ~ via pathexpand)
  ssh_public_key_from_file = var.ssh_public_key_file != "" ? trimspace(file(pathexpand(var.ssh_public_key_file))) : ""
  effective_ssh_public_key = trimspace(var.ssh_public_key != "" ? var.ssh_public_key : local.ssh_public_key_from_file)

  ssh_metadata = local.effective_ssh_public_key != "" ? {
    "ssh-keys" = "${var.ssh_username}:${local.effective_ssh_public_key}"
  } : {}
}

resource "yandex_compute_instance" "master" {
  name        = var.master_vm_name
  hostname    = var.master_vm_name
  zone        = var.yc_zone
  platform_id = "standard-v3"

  resources {
    cores         = var.master_vm_cores
    memory        = var.master_vm_memory
    core_fraction = 20
  }

  scheduling_policy {
    preemptible = true
  }

  boot_disk {
    initialize_params {
      image_id = data.yandex_compute_image.centos_stream9_oslogin.id
      size     = var.master_vm_disk_size
      type     = "network-hdd"
    }
  }

  network_interface {
    subnet_id      = yandex_vpc_subnet.internal.id
    nat            = true
    nat_ip_address = yandex_vpc_address.master.external_ipv4_address[0].address
  }

  metadata = merge(local.base_metadata, local.ssh_metadata)
}

resource "yandex_compute_instance" "workers" {
  for_each    = { for w in var.workers : w.vm_name => w }
  name        = each.value.vm_name
  hostname    = each.value.vm_name
  zone        = var.yc_zone
  platform_id = "standard-v3"

  resources {
    cores         = var.worker_vm_cores
    memory        = var.worker_vm_memory
    core_fraction = 20
  }

  scheduling_policy {
    preemptible = true
  }

  boot_disk {
    initialize_params {
      image_id = data.yandex_compute_image.centos_stream9_oslogin.id
      size     = var.worker_vm_disk_size
      type     = "network-hdd"
    }
  }

  network_interface {
    subnet_id      = yandex_vpc_subnet.internal.id
    nat            = true
    nat_ip_address = yandex_vpc_address.workers[each.key].external_ipv4_address[0].address
  }

  metadata = merge(local.base_metadata, local.ssh_metadata)
}
