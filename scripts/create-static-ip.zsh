#!/usr/bin/env zsh
# Reserve a static public IP address in a given zone (idempotent)
# Usage:
#   [YC_ADDRESS_NAME=ajasta-static-ip] [YC_ZONE=ru-central1-b] ./create-static-ip.zsh
# Output: echoes the allocated IPv4 address

set -euo pipefail
SCRIPT_DIR=$(cd -- "${0:A:h}" && pwd)
source "$SCRIPT_DIR/yc-common.zsh"
require_cmd yc jq

YC_ADDRESS_NAME=${YC_ADDRESS_NAME:-ajasta-static-ip}

log "Ensuring static address '$YC_ADDRESS_NAME' is absent (to recreate fresh)..."
delete_address_by_name "$YC_ADDRESS_NAME" || true

log "Creating static address '$YC_ADDRESS_NAME' in zone $YC_ZONE..."
ADDR_JSON=$(yc vpc address create \
  --name "$YC_ADDRESS_NAME" \
  --external-ipv4 zone="$YC_ZONE" \
  --folder-id "$YC_FOLDER_ID" \
  --format json)

IP=$(echo "$ADDR_JSON" | jq -r '.external_ipv4_address.address')
ID=$(echo "$ADDR_JSON" | jq -r '.id')
log "Created static IP: $IP (ID: $ID)"

echo "$IP"
