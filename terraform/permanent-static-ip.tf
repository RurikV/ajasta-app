# Permanent Static IP Address for Production DNS
# This creates a dedicated, permanently reserved IP address for ajasta.top domain
# This IP is independent of VM lifecycle and will never be released automatically

resource "yandex_vpc_address" "production_dns" {
  name = "ajasta-production-dns-ip"

  labels = {
    environment = "production"
    purpose     = "dns-static-ip"
    domain      = "ajasta.top"
    managed-by  = "terraform"
    permanent   = "true"
  }

  external_ipv4_address {
    zone_id = var.yc_zone
  }

  # Ensure this IP is never destroyed accidentally
  lifecycle {
    prevent_destroy = true
  }
}

# Output the permanent IP address
output "permanent_static_ip" {
  description = "Permanent static IP address for ajasta.top DNS (178.154.197.121)"
  value       = try(yandex_vpc_address.production_dns.external_ipv4_address[0].address, "178.154.197.121")
}

# Output the permanent IP address ID
output "permanent_static_ip_id" {
  description = "ID of the permanent static IP address resource"
  value       = try(yandex_vpc_address.production_dns.id, "not_created")
}
