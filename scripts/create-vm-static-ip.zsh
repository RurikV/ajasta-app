#!/usr/bin/env zsh
# Create a VM instance with a reserved static public IP (idempotent)
# Usage:
#   ./create-vm-static-ip.zsh <vm_name>
# Env overrides:
#   YC_SUBNET_NAME (default: ajasta-external-segment)
#   YC_ADDRESS_NAME (default: ajasta-static-ip)
#   METADATA_YAML (default: ./metadata.yaml)
#   SSH_USERNAME (optional; if set with a pubkey, authorizes this user; default: ubuntu)
#   SSH_PUBKEY_FILE or SSH_PUBKEY (optional; path or content of public key)

set -euo pipefail
SCRIPT_DIR=$(cd -- "${0:A:h}" && pwd)
source "$SCRIPT_DIR/yc-common.zsh"
require_cmd yc jq

if [ ${#} -lt 1 ]; then
  echo "Usage: $0 <vm_name>" >&2
  exit 2
fi

vm_name="$1"
YC_SUBNET_NAME=${YC_SUBNET_NAME:-ajasta-external-segment}
YC_ADDRESS_NAME=${YC_ADDRESS_NAME:-ajasta-static-ip}
METADATA_YAML=${METADATA_YAML:-./metadata.yaml}
SSH_USERNAME=${SSH_USERNAME:-ubuntu}

if [ ! -f "$METADATA_YAML" ]; then
  log "Metadata file '$METADATA_YAML' not found" >&2
  exit 1
fi

# Prepare optional ssh-keys metadata
SSH_META_ARGS=()
if [ -n "${SSH_PUBKEY_FILE:-}" ] || [ -n "${SSH_PUBKEY:-}" ]; then
  if [ -n "${SSH_PUBKEY_FILE:-}" ]; then
    if [ ! -f "$SSH_PUBKEY_FILE" ]; then
      log "SSH_PUBKEY_FILE '$SSH_PUBKEY_FILE' not found" >&2
      exit 1
    fi
    PUBKEY_CONTENT=$(cat "$SSH_PUBKEY_FILE")
  else
    PUBKEY_CONTENT="$SSH_PUBKEY"
  fi
  SSH_META_ARGS=( --metadata "ssh-keys=${SSH_USERNAME}:${PUBKEY_CONTENT}")
  log "Will inject SSH key for user '${SSH_USERNAME}'."
fi

log "Ensuring instance '$vm_name' is absent (to recreate fresh)..."
delete_instance_by_name "$vm_name" || true
wait_instance_absent "$vm_name" 60 5 || true

log "Ensuring static address '$YC_ADDRESS_NAME' is absent (to recreate fresh)..."
delete_address_by_name "$YC_ADDRESS_NAME" || true

log "Creating static IP '$YC_ADDRESS_NAME' in zone $YC_ZONE..."
ADDR_JSON=$(yc vpc address create \
  --name "$YC_ADDRESS_NAME" \
  --external-ipv4 zone="$YC_ZONE" \
  --folder-id "$YC_FOLDER_ID" \
  --format json)

STATIC_IP_ADDRESS=$(echo "$ADDR_JSON" | jq -r '.external_ipv4_address.address')
STATIC_IP_ID=$(echo "$ADDR_JSON" | jq -r '.id')
log "Created static IP: $STATIC_IP_ADDRESS (ID: $STATIC_IP_ID)"

log "Creating VM '$vm_name' using static IP $STATIC_IP_ADDRESS..."
yc compute instance create \
  --preemptible \
  --name "$vm_name" \
  --hostname "$vm_name" \
  --zone "$YC_ZONE" \
  --memory=2 \
  --cores=2 \
  --create-boot-disk image-folder-id=standard-images,image-family=nat-instance-ubuntu-2204,type=network-hdd,size=10 \
  --network-interface subnet-name="$YC_SUBNET_NAME",nat-ip-version=ipv4,nat-address="$STATIC_IP_ADDRESS" \
  --serial-port-settings ssh-authorization=INSTANCE_METADATA \
  --metadata-from-file user-data="$METADATA_YAML" \
  ${SSH_META_ARGS:+${SSH_META_ARGS[@]}}

log "VM '$vm_name' created with static IP $STATIC_IP_ADDRESS."

echo "$STATIC_IP_ADDRESS"
