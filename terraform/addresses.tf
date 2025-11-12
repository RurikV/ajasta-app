resource "yandex_vpc_address" "master" {
  name = var.master_address_name
  external_ipv4_address {
    zone_id = var.yc_zone
  }
}

resource "yandex_vpc_address" "workers" {
  for_each = { for w in var.workers : w.vm_name => w }
  name = each.value.address_name
  external_ipv4_address {
    zone_id = var.yc_zone
  }
}
