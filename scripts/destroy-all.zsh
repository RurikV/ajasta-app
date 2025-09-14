#!/usr/bin/env zsh
# Destroy all Yandex Cloud resources created by this project
# This script is idempotent: it safely deletes resources if they exist.
# Order of deletion:
#   1) VM instances (ajasta-host, vm1-host, vm2-host)
#   2) Static public IP address
#   3) Subnets (external and internal)
#   4) Networks (external and internal)
#   5) Service Account
#   6) Optional local artifacts cleanup (sa-key.json, SSH keys)
#
# Usage:
#   ./destroy-all.zsh
#
# Environment overrides (defaults reflect project scripts):
#   ajasta_VM_NAME=ajasta-host
#   VM1_NAME=vm1-host
#   VM2_NAME=vm2-host
#   YC_ADDRESS_NAME=ajasta-static-ip
#   EXT_NET_NAME=external-ajasta-network
#   EXT_SUBNET_NAME=ajasta-external-segment
#   INT_NET_NAME=internal-ajasta-network
#   INT_SUBNET_NAME=ajasta-internal-segment
#   YC_SA_NAME=otus
#   CLEAN_LOCAL_KEYS=false — set to true to remove ./ajasta_ed25519[.pub], ./vm1_ed25519[.pub], ./vm2_ed25519[.pub]
#   CLEAN_SA_KEY=true — remove sa-key.json if present
#
# Requires: zsh, yc, jq

set -euo pipefail
SCRIPT_DIR=$(cd -- "${0:A:h}" && pwd)
source "$SCRIPT_DIR/yc-common.zsh"
require_cmd yc jq

# Defaults
ajasta_VM_NAME=${ajasta_VM_NAME:-ajasta-host}
VM1_NAME=${VM1_NAME:-vm1-host}
VM2_NAME=${VM2_NAME:-vm2-host}
YC_ADDRESS_NAME=${YC_ADDRESS_NAME:-ajasta-static-ip}
EXT_NET_NAME=${EXT_NET_NAME:-external-ajasta-network}
EXT_SUBNET_NAME=${EXT_SUBNET_NAME:-ajasta-external-segment}
INT_NET_NAME=${INT_NET_NAME:-internal-ajasta-network}
INT_SUBNET_NAME=${INT_SUBNET_NAME:-ajasta-internal-segment}
YC_SA_NAME=${YC_SA_NAME:-otus}
CLEAN_LOCAL_KEYS=${CLEAN_LOCAL_KEYS:-false}
CLEAN_SA_KEY=${CLEAN_SA_KEY:-true}

log "Starting teardown of project resources in folder $YC_FOLDER_ID (zone $YC_ZONE)"

# 1) Delete VM(s)
log "Ensuring instance '$ajasta_VM_NAME' is deleted..."
delete_instance_by_name "$ajasta_VM_NAME" || true
wait_instance_absent "$ajasta_VM_NAME" 60 5 || true

log "Ensuring instance '$VM1_NAME' is deleted..."
delete_instance_by_name "$VM1_NAME" || true
wait_instance_absent "$VM1_NAME" 60 5 || true

log "Ensuring instance '$VM2_NAME' is deleted..."
delete_instance_by_name "$VM2_NAME" || true
wait_instance_absent "$VM2_NAME" 60 5 || true

# 2) Delete static IP address
log "Ensuring static address '$YC_ADDRESS_NAME' is deleted..."
delete_address_by_name "$YC_ADDRESS_NAME" || true

# 3) Delete subnets (internal then external, order doesn't matter but be explicit)
log "Ensuring subnet '$INT_SUBNET_NAME' is deleted..."
delete_subnet_by_name "$INT_SUBNET_NAME" || true
log "Ensuring subnet '$EXT_SUBNET_NAME' is deleted..."
delete_subnet_by_name "$EXT_SUBNET_NAME" || true

# 4) Delete networks (internal then external)
log "Ensuring network '$INT_NET_NAME' is deleted..."
delete_network_by_name "$INT_NET_NAME" || true
log "Ensuring network '$EXT_NET_NAME' is deleted..."
delete_network_by_name "$EXT_NET_NAME" || true

# 5) Delete service account
log "Ensuring service account '$YC_SA_NAME' is deleted..."
delete_sa_by_name "$YC_SA_NAME" || true

# 6) Local artifacts cleanup
if [ "$CLEAN_SA_KEY" = "true" ] && [ -f "$SCRIPT_DIR/sa-key.json" ]; then
  log "Removing local file sa-key.json"
  rm -f "$SCRIPT_DIR/sa-key.json" || true
fi

if [ "$CLEAN_LOCAL_KEYS" = "true" ]; then
  # Remove ajasta SSH keys
  if [ -f "$SCRIPT_DIR/ajasta_ed25519" ] || [ -f "$SCRIPT_DIR/ajasta_ed25519.pub" ]; then
    log "Removing local SSH keypair ./ajasta_ed25519(.pub)"
    rm -f "$SCRIPT_DIR/ajasta_ed25519" "$SCRIPT_DIR/ajasta_ed25519.pub" || true
  fi
  
  # Remove vm1 SSH keys
  if [ -f "$SCRIPT_DIR/vm1_ed25519" ] || [ -f "$SCRIPT_DIR/vm1_ed25519.pub" ]; then
    log "Removing local SSH keypair ./vm1_ed25519(.pub)"
    rm -f "$SCRIPT_DIR/vm1_ed25519" "$SCRIPT_DIR/vm1_ed25519.pub" || true
  fi
  
  # Remove vm2 SSH keys
  if [ -f "$SCRIPT_DIR/vm2_ed25519" ] || [ -f "$SCRIPT_DIR/vm2_ed25519.pub" ]; then
    log "Removing local SSH keypair ./vm2_ed25519(.pub)"
    rm -f "$SCRIPT_DIR/vm2_ed25519" "$SCRIPT_DIR/vm2_ed25519.pub" || true
  fi
  
  log "Local SSH key cleanup completed."
else
  log "CLEAN_LOCAL_KEYS is false; keeping local SSH keys."
fi

log "Teardown completed."
