resource "yandex_vpc_network" "external" {
  name = var.yc_network_name
}

resource "yandex_vpc_subnet" "external" {
  name           = var.yc_subnet_name
  zone           = var.yc_zone
  network_id     = yandex_vpc_network.external.id
  v4_cidr_blocks = [var.yc_subnet_cidr]
}

resource "yandex_vpc_network" "internal" {
  name = var.yc_internal_network_name
}

resource "yandex_vpc_subnet" "internal" {
  name           = var.yc_internal_subnet_name
  zone           = var.yc_zone
  network_id     = yandex_vpc_network.internal.id
  v4_cidr_blocks = [var.yc_internal_subnet_cidr]
}
