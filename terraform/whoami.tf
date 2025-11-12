data "yandex_client_config" "this" {}

output "current_context" {
  value = {
    cloud_id  = data.yandex_client_config.this.cloud_id
    folder_id = data.yandex_client_config.this.folder_id
    zone      = data.yandex_client_config.this.zone
  }
  description = "Yandex provider context (helps debug auth/folder mismatches)"
}
