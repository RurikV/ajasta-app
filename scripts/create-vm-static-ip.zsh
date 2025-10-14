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
#   VM_MEMORY (default: 4) - Memory in GB for the VM
#   VM_CORES (default: 2) - CPU cores for the VM
#   VM_DISK_SIZE (default: 30) - Boot disk size in GB for the VM

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
SSH_USERNAME=${SSH_USERNAME:-ajasta}
VM_MEMORY=${VM_MEMORY:-4}
VM_CORES=${VM_CORES:-2}
VM_DISK_SIZE=${VM_DISK_SIZE:-30}

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
  
  # Replace DOCKERHUB_USER placeholder in the containerized metadata
  sed "s/\${DOCKERHUB_USER:-vladimirryrik}/$DOCKERHUB_USER/g" "$METADATA_YAML" > "$TEMP_USER_DATA"
  
  # If SSH key is provided, we need to add it to the temp file
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
  # Original SSH key injection logic for non-containerized deployments
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
  - name: ${SSH_USERNAME}
    groups: [wheel, sudo]
    shell: /bin/bash
    sudo: ['ALL=(ALL) NOPASSWD:ALL']
    ssh_authorized_keys:
      - ${PUBKEY_CONTENT}
write_files:
  - path: /etc/sudoers.d/90-${SSH_USERNAME}
    content: '${SSH_USERNAME} ALL=(ALL) NOPASSWD:ALL'
    owner: root:root
    permissions: '0440'
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
    # Get the current static IP from the existing VM
    EXISTING_IP=$(yc compute instance get --name "$vm_name" --format json | jq -r '.network_interfaces[0].primary_v4_address.one_to_one_nat.address // empty')
    if [ -n "$EXISTING_IP" ]; then
      log "VM '$vm_name' already has static IP: $EXISTING_IP. Reusing existing setup."
      echo "$EXISTING_IP"
      exit 0
    else
      log "Warning: VM '$vm_name' exists but has no static IP. Will recreate to fix configuration."
    fi
  else
    log "Instance '$vm_name' exists but is not running (status: $VM_STATUS). Will recreate."
  fi
  # If we reach here, the VM exists but needs to be recreated
  log "Deleting existing instance '$vm_name' to recreate with proper configuration..."
  delete_instance_by_name "$vm_name" || true
  wait_instance_absent "$vm_name" 60 5 || true
else
  log "Instance '$vm_name' does not exist. Will create new VM."
fi

# Get or create static IP (reuse existing logic from create-static-ip.zsh)
if address_exists "$YC_ADDRESS_NAME"; then
  log "Static address '$YC_ADDRESS_NAME' already exists, reusing it."
  ADDR_JSON=$(yc vpc address get --name "$YC_ADDRESS_NAME" --format json)
  STATIC_IP_ADDRESS=$(echo "$ADDR_JSON" | jq -r '.external_ipv4_address.address')
  STATIC_IP_ID=$(echo "$ADDR_JSON" | jq -r '.id')
  log "Reusing static IP: $STATIC_IP_ADDRESS (ID: $STATIC_IP_ID)"
else
  log "Creating static IP '$YC_ADDRESS_NAME' in zone $YC_ZONE..."
  ADDR_JSON=$(yc vpc address create \
    --name "$YC_ADDRESS_NAME" \
    --external-ipv4 zone="$YC_ZONE" \
    --folder-id "$YC_FOLDER_ID" \
    --format json)
  
  STATIC_IP_ADDRESS=$(echo "$ADDR_JSON" | jq -r '.external_ipv4_address.address')
  STATIC_IP_ID=$(echo "$ADDR_JSON" | jq -r '.id')
  log "Created static IP: $STATIC_IP_ADDRESS (ID: $STATIC_IP_ID)"
fi

log "Creating VM '$vm_name' using static IP $STATIC_IP_ADDRESS (Memory: ${VM_MEMORY}GB, Cores: ${VM_CORES}, Disk: ${VM_DISK_SIZE}GB)..."
yc compute instance create \
  --preemptible \
  --name "$vm_name" \
  --hostname "$vm_name" \
  --zone "$YC_ZONE" \
  --memory="$VM_MEMORY" \
  --cores="$VM_CORES" \
  --core-fraction=20 \
  --create-boot-disk image-folder-id=standard-images,image-family=centos-stream-9-oslogin,type=network-hdd,size="$VM_DISK_SIZE" \
  --network-interface subnet-name="$YC_SUBNET_NAME",nat-ip-version=ipv4,nat-address="$STATIC_IP_ADDRESS" \
  --serial-port-settings ssh-authorization=INSTANCE_METADATA \
  --metadata-from-file user-data="${TEMP_USER_DATA:-$METADATA_YAML}" \
  ${SSH_META_ARGS:+${SSH_META_ARGS[@]}}

log "VM '$vm_name' created with static IP $STATIC_IP_ADDRESS."

echo "$STATIC_IP_ADDRESS"
