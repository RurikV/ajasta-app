#!/usr/bin/env zsh
# Create a proper Yandex Cloud IAM service account key JSON for Terraform
# Usage:
#   scripts/create-iam-terraform-key.zsh <sa-name> [output-file]
# Defaults:
#   <sa-name>     = ajasta-tf
#   [output-file] = ../sa-iam-key.json (relative to this script location)

set -euo pipefail

SCRIPT_DIR=$(cd -- "${0:A:h}" && pwd)

SA_NAME=${1:-ajasta-tf}
OUT_FILE=${2:-"$SCRIPT_DIR/../sa-iam-key.json"}

require_cmd() {
  for c in "$@"; do
    command -v "$c" >/dev/null 2>&1 || { echo "Error: required command '$c' not found in PATH" >&2; exit 1; }
  done
}

log() { echo "[create-iam-terraform-key] $*"; }

require_cmd yc

# Ensure we can talk to YC
if ! yc config list >/dev/null 2>&1; then
  echo "YC CLI is not initialized. Run 'yc init' first." >&2
  exit 1
fi

log "Ensuring service account '$SA_NAME' exists..."
if ! yc iam service-account get --name "$SA_NAME" >/dev/null 2>&1; then
  yc iam service-account create --name "$SA_NAME" >/dev/null
  log "Created service account '$SA_NAME'"
else
  log "Service account '$SA_NAME' already exists"
fi

SA_ID=$(yc iam service-account get --name "$SA_NAME" --format json | sed -n 's/.*"id": "\([^"]*\)".*/\1/p' | head -n1)
if [ -z "$SA_ID" ]; then
  echo "Failed to retrieve service account id for '$SA_NAME'" >&2
  exit 1
fi
log "Service account id: $SA_ID"

# Protect against accidental overwrite
if [ -f "$OUT_FILE" ]; then
  log "Output file '$OUT_FILE' already exists. Refusing to overwrite."
  log "Delete it or pass a different path as the second argument."
  exit 2
fi

log "Creating IAM key JSON at '$OUT_FILE'..."
yc iam key create --service-account-id "$SA_ID" --output "$OUT_FILE"

log "Done. Wrote IAM key JSON to: $OUT_FILE"
log "Next: set yc_service_account_key_file = \"${OUT_FILE##*$SCRIPT_DIR/..}\" in terraform/terraform.tfvars, or simply:"
log "  yc_service_account_key_file = \"../sa-iam-key.json\""

exit 0
