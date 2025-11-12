output "master_public_ip" {
  description = "Master node public IP"
  value       = yandex_compute_instance.master.network_interface[0].nat_ip_address
}

output "worker_public_ips" {
  description = "Map of worker name => public IP"
  value = { for k, inst in yandex_compute_instance.workers : k => inst.network_interface[0].nat_ip_address }
}

output "master_instance_id" {
  value       = yandex_compute_instance.master.id
  description = "Master instance ID"
}

output "worker_instance_ids" {
  value       = { for k, inst in yandex_compute_instance.workers : k => inst.id }
  description = "Map of worker name => instance ID"
}
