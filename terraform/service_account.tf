resource "yandex_iam_service_account" "this" {
  count = var.create_service_account ? 1 : 0
  name  = var.yc_sa_name
}

# Optional static key for the service account (JSON)
resource "yandex_iam_service_account_key" "this" {
  count              = var.create_service_account ? 1 : 0
  service_account_id = yandex_iam_service_account.this[0].id
}

output "service_account_id" {
  value       = var.create_service_account ? yandex_iam_service_account.this[0].id : null
  description = "Service account ID (if created)"
}

output "service_account_key_json" {
  value       = var.create_service_account ? yandex_iam_service_account_key.this[0].private_key : null
  sensitive   = true
  description = "Service account key JSON (sensitive, if created)"
}
