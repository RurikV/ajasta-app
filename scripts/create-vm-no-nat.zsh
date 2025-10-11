#!/usr/bin/env zsh
# Create a VM instance without a public IP (no NAT), attached to a given subnet (idempotent)
# Usage:
#   ./create-vm-no-nat.zsh <vm_name>
# Env overrides:
#   YC_SUBNET_NAME (required)
#   METADATA_YAML (default: ./metadata.yaml)
#   SSH_USERNAME (optional; if set with a pubkey, authorizes this user; default: ubuntu)
#   SSH_PUBKEY_FILE or SSH_PUBKEY (optional; path or content of public key)
#
# Notes:
# - Reuses helper functions from yc-common.zsh
# - If VM exists and is RUNNING, it is reused as-is.
# - If VM exists but is not running or misconfigured, it is recreated.

set -euo pipefail
SCRIPT_DIR=$(cd -- "${0:A:h}" && pwd)
source "$SCRIPT_DIR/yc-common.zsh"
require_cmd yc jq

if [ ${#} -lt 1 ]; then
  echo "Usage: $0 <vm_name>" >&2
  exit 2
fi

vm_name="$1"
YC_SUBNET_NAME=${YC_SUBNET_NAME:-}
METADATA_YAML=${METADATA_YAML:-./metadata.yaml}
SSH_USERNAME=${SSH_USERNAME:-ajasta}

if [ -z "$YC_SUBNET_NAME" ]; then
  log "YC_SUBNET_NAME must be provided via environment" >&2
  exit 1
fi

if [ ! -f "$METADATA_YAML" ]; then
  log "Metadata file '$METADATA_YAML' not found" >&2
  exit 1
fi

# Prepare optional ssh key and, if present, build a dedicated cloud-init user-data file
TEMP_USER_DATA=""
SSH_META_ARGS=()

# Check if we need to inject DOCKERHUB_USER into containerized metadata
if [[ "$METADATA_YAML" == *"containerized"* ]] && [ -n "${DOCKERHUB_USER:-}" ]; then
  log "Injecting DOCKERHUB_USER='$DOCKERHUB_USER' into containerized metadata..."
  TEMP_USER_DATA="$SCRIPT_DIR/.tmp-user-data-${vm_name}.yaml"
  sed "s/\${DOCKERHUB_USER:-vladimirryrik}/$DOCKERHUB_USER/g" "$METADATA_YAML" > "$TEMP_USER_DATA"
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
    SSH_META_ARGS=( --metadata "ssh-keys=${SSH_USERNAME}:${PUBKEY_CONTENT}" )
    log "Will seed SSH keys and DOCKERHUB_USER for containerized deployment."
  else
    log "Will seed DOCKERHUB_USER='$DOCKERHUB_USER' for containerized deployment."
  fi
elif [ -n "${SSH_PUBKEY_FILE:-}" ] || [ -n "${SSH_PUBKEY:-}" ]; then
  if [ -n "${SSH_PUBKEY_FILE:-}" ]; then
    if [ ! -f "$SSH_PUBKEY_FILE" ]; then
      log "SSH_PUBKEY_FILE '$SSH_PUBKEY_FILE' not found" >&2
      exit 1
    fi
    PUBKEY_CONTENT=$(cat "$SSH_PUBKEY_FILE")
  else
    PUBKEY_CONTENT="$SSH_PUBKEY"
  fi
  TEMP_USER_DATA="$SCRIPT_DIR/.tmp-user-data-${vm_name}.yaml"
  cat > "$TEMP_USER_DATA" <<EOF
#cloud-config
users:
  - name: ubuntu
    groups: sudo
    shell: /bin/bash
    sudo: ['ALL=(ALL) NOPASSWD:ALL']
  - name: ${SSH_USERNAME}
    groups: sudo
    shell: /bin/bash
    sudo: ['ALL=(ALL) NOPASSWD:ALL']
    ssh_authorized_keys:
      - ${PUBKEY_CONTENT}
EOF
  SSH_META_ARGS=( --metadata "ssh-keys=${SSH_USERNAME}:${PUBKEY_CONTENT}" )
  log "Will seed cloud-init authorized_keys and instance metadata for user '${SSH_USERNAME}'."
fi

# Check if VM already exists and reuse it if possible
if instance_exists "$vm_name"; then
  log "Instance '$vm_name' already exists. Checking if it's running and accessible..."
  VM_STATUS=$(yc compute instance get --name "$vm_name" --format json | jq -r '.status')
  if [ "$VM_STATUS" = "RUNNING" ]; then
    log "Instance '$vm_name' is already running. Reusing existing VM."
    # No public IP to print; show internal IP for visibility
    INTERNAL_IP=$(yc compute instance get --name "$vm_name" --format json | jq -r '.network_interfaces[0].primary_v4_address.address // empty')
    if [ -n "$INTERNAL_IP" ]; then
      log "VM '$vm_name' internal IP: $INTERNAL_IP"
      echo "$INTERNAL_IP"
    fi
    exit 0
  else
    log "Instance '$vm_name' exists but is not running (status: $VM_STATUS). Will recreate."
  fi
  log "Deleting existing instance '$vm_name' to recreate without public IP..."
  delete_instance_by_name "$vm_name" || true
  wait_instance_absent "$vm_name" 60 5 || true
else
  log "Instance '$vm_name' does not exist. Will create new VM."
fi

log "Creating VM '$vm_name' without public IP on subnet '$YC_SUBNET_NAME'..."
yc compute instance create \
  --preemptible \
  --name "$vm_name" \
  --hostname "$vm_name" \
  --zone "$YC_ZONE" \
  --memory=2 \
  --cores=2 \
  --create-boot-disk image-folder-id=standard-images,image-family=ubuntu-2204-lts,type=network-hdd,size=10 \
  --network-interface subnet-name="$YC_SUBNET_NAME" \
  --serial-port-settings ssh-authorization=INSTANCE_METADATA \
  --metadata-from-file user-data="${TEMP_USER_DATA:-$METADATA_YAML}" \
  ${SSH_META_ARGS:+${SSH_META_ARGS[@]}}

log "VM '$vm_name' created without public IP."

# Output internal IP for convenience
INTERNAL_IP=$(yc compute instance get --name "$vm_name" --format json | jq -r '.network_interfaces[0].primary_v4_address.address // empty')
[ -n "$INTERNAL_IP" ] && echo "$INTERNAL_IP" || true
